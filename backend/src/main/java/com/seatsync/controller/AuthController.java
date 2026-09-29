package com.seatsync.controller;

import com.seatsync.dto.auth.LoginRequest;
import com.seatsync.dto.auth.LoginResponse;
import com.seatsync.dto.auth.RegisterRequest;
import com.seatsync.dto.auth.UserResponse;
import com.seatsync.security.AuthCookieFactory;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final AuthCookieFactory cookieFactory;

    public AuthController(AuthService authService, AuthCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withSessionCookie(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSessionCookie(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.getProfile(user.id());
    }

    private ResponseEntity<LoginResponse> withSessionCookie(HttpStatus status, LoginResponse response) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(response.token(), response.expiresAt()).toString())
                .body(response);
    }
}
