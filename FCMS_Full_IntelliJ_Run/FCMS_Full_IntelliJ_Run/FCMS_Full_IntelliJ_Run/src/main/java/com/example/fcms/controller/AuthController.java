package com.example.fcms.controller;

import com.example.fcms.dto.auth.AuthResponse;
import com.example.fcms.dto.auth.ForgotPasswordRequest;
import com.example.fcms.dto.auth.LoginRequest;
import com.example.fcms.dto.auth.RegisterRequest;
import com.example.fcms.dto.auth.ResetPasswordRequest;
import com.example.fcms.dto.auth.VerifyOtpRequest;
import com.example.fcms.entity.User;
import com.example.fcms.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        User user = authService.login(request);

        session.setAttribute("currentUserId", user.getUserId());
        session.setAttribute("currentUserRole", user.getRole());

        AuthResponse response = new AuthResponse("Login successful", user.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        session.invalidate();

        AuthResponse response = new AuthResponse("Logout successful", null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        AuthResponse response = authService.sendForgotPasswordOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpSession session
    ) {
        AuthResponse response = authService.verifyOtp(request);

        session.setAttribute("resetPasswordEmail", request.getEmail().trim().toLowerCase());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpSession session
    ) {
        String email = (String) session.getAttribute("resetPasswordEmail");

        if (email == null) {
            throw new SecurityException("Please verify OTP first");
        }

        AuthResponse response = authService.resetPassword(email, request);
        session.removeAttribute("resetPasswordEmail");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/google/complete")
    public ResponseEntity<AuthResponse> completeGoogleRegistration(
            @RequestParam String role,
            HttpSession session
    ) {
        String email = (String) session.getAttribute("pendingGoogleEmail");
        String name = (String) session.getAttribute("pendingGoogleName");
        String avatar = (String) session.getAttribute("pendingGoogleAvatar");

        if (email == null) {
            throw new SecurityException("Google login session expired");
        }

        User user = authService.completeGoogleRegistration(email, name, avatar, role);

        session.removeAttribute("pendingGoogleEmail");
        session.removeAttribute("pendingGoogleName");
        session.removeAttribute("pendingGoogleAvatar");

        session.setAttribute("currentUserId", user.getUserId());
        session.setAttribute("currentUserRole", user.getRole());

        AuthResponse response = new AuthResponse("Google account created", user.getRole());
        return ResponseEntity.ok(response);
    }
}
