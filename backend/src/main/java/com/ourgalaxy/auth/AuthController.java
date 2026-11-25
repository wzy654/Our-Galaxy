package com.ourgalaxy.auth;

import com.ourgalaxy.auth.dto.LoginRequest;
import com.ourgalaxy.auth.dto.RegisterRequest;
import com.ourgalaxy.auth.dto.TokenResponse;
import com.ourgalaxy.auth.dto.UserVO;
import com.ourgalaxy.common.result.ApiResponse;
import com.ourgalaxy.common.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        return ApiResponse.success(authService.login(request, ip));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(authService.me(principal.userId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal JwtPrincipal principal) {
        authService.logout(principal.userId(), principal.tokenId());
        return ApiResponse.success();
    }
}
