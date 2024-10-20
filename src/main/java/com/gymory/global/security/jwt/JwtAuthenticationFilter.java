package com.gymory.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymory.domain.user.userbase.dto.UserDto;
import com.gymory.domain.user.userbase.service.UserService;
import com.gymory.global.security.AES128Service;
import com.gymory.global.redis.RedisUtils;
import com.gymory.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AES128Service aes128Service;
    private final UserService userService;
    private final RedisUtils redisUtils;

    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        // ServletInputStream을 LoginDto 객체로 역직렬화
        ObjectMapper objectMapper = new ObjectMapper();
        UserDto userDto = objectMapper.readValue(request.getInputStream(), UserDto.class);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword());

        return authenticationManager.authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(customUserDetails);
        String accessToken = tokenDto.getAccessToken();
        String refreshToken = tokenDto.getRefreshToken();
        String encryptedRefreshToken = aes128Service.encryptAes(refreshToken);

        jwtTokenProvider.accessTokenSetHeader(accessToken, response);
        jwtTokenProvider.refresshTokenSetHeader(encryptedRefreshToken, response);

        UserDto findUser = userService
                .findUserAndUpdateTokens(customUserDetails.getId(), accessToken, refreshToken);
        log.info("login success = {}", findUser);

        // 로그인 성공시 Refresh Token Redis 저장 ( key = Email / value = Refresh Token )
        long refreshTokenExpirationMillis = jwtTokenProvider.getRefreshTokenExpirationMillis();
        redisUtils.setData(findUser.getEmail(), refreshToken, refreshTokenExpirationMillis);

        this.getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
    }
}