package com.ers.security.web;

import com.ers.common.web.ApiResponse;
import com.ers.security.dto.ChangePasswordRequest;
import com.ers.security.dto.CurrentUserResponse;
import com.ers.security.dto.LoginRequest;
import com.ers.security.dto.RefreshRequest;
import com.ers.security.dto.TokenResponse;
import com.ers.security.jwt.JwtTokenProvider;
import com.ers.security.service.AuthService;
import com.ers.security.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public AuthController(AuthService authService, JwtTokenProvider tokenProvider, UserService userService) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing bearer token");
        }
        return ApiResponse.ok(authService.currentUserFromClaims(tokenProvider.parseClaims(header.substring(7))));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        userService.changeOwnPassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return ApiResponse.ok(null);
    }
}
