package com.gymory.domain.test;

import com.gymory.domain.user.base.UserPermission;
import com.gymory.global.aop.RefreshTokenAspect;
import com.gymory.global.aop.ValidateRefreshToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @ValidateRefreshToken(role = UserPermission.ADMIN)
    @GetMapping("/hello")
    public String hello() {
        String email = RefreshTokenAspect.getEmail();
        return "Hello, " + email + "!";
    }
}
