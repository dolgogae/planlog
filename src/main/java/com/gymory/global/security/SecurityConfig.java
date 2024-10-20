package com.gymory.global.security;

import com.gymory.domain.user.userbase.service.UserService;
import com.gymory.global.redis.RedisUtils;
import com.gymory.global.security.handler.CustomAccessDeniedHandler;
import com.gymory.global.security.handler.LoginFailureHandler;
import com.gymory.global.security.handler.LoginSuccessHandler;
import com.gymory.global.security.jwt.JwtTokenProvider;
import com.gymory.global.security.jwt.JwtVerificationFilter;
import com.gymory.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final AES128Service aes128Service;
    private final RedisUtils redisUtils;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        
        http
                .headers(header -> header.frameOptions().sameOrigin())
                .csrf(AbstractHttpConfigurer::disable)
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .authorizeHttpRequests(auth -> auth
                        .antMatchers(
                                "/","/**",
                                "/h2-console",
                                "/login/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/auth/**").permitAll()
                        .antMatchers("/user/**").hasAnyRole("ADMIN", "USER", "ANONYMOUS")
                        .antMatchers("/admin/**").hasAnyRole("ADMIN")
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .exceptionHandling(exception
                        -> exception.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()))
                .apply(new CustomFilterConfigurer());

        
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE"));
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Refresh");
        configuration.addAllowedHeader("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    public class CustomFilterConfigurer extends AbstractHttpConfigurer<CustomFilterConfigurer, HttpSecurity> {
        @Override
        public void configure(HttpSecurity builder) throws Exception {
            log.info("SecurityConfiguration.CustomFilterConfigurer.configure excute");
            AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);
            JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager,
                    jwtTokenProvider, aes128Service, userService, redisUtils);
            JwtVerificationFilter jwtVerificationFilter = new JwtVerificationFilter(jwtTokenProvider, redisUtils);

            jwtAuthenticationFilter.setFilterProcessesUrl("/auth/login");
            jwtAuthenticationFilter.setAuthenticationSuccessHandler(new LoginSuccessHandler());
            jwtAuthenticationFilter.setAuthenticationFailureHandler(new LoginFailureHandler());

            builder
                    .addFilter(jwtAuthenticationFilter)
                    .addFilterAfter(jwtVerificationFilter, JwtAuthenticationFilter.class);
        }
    }
}
