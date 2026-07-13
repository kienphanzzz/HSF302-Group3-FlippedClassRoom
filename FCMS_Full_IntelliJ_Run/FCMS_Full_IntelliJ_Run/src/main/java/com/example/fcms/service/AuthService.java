package com.example.fcms.service;

import com.example.fcms.dto.auth.AuthResponse;
import com.example.fcms.dto.auth.ForgotPasswordRequest;
import com.example.fcms.dto.auth.LoginRequest;
import com.example.fcms.dto.auth.RegisterRequest;
import com.example.fcms.dto.auth.ResetPasswordRequest;
import com.example.fcms.dto.auth.VerifyOtpRequest;
import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status("ACTIVE")
                .build();

        userRepository.save(user);

        return new AuthResponse("Account created successfully", user.getRole());
    }

    public User login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("Invalid email or password"));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            throw new SecurityException("Invalid email or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new SecurityException("This account is not active");
        }

        return user;
    }

    public AuthResponse sendForgotPasswordOtp(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email does not exist");
        }

        String otp = otpService.createOtp(email);
        emailService.sendPasswordResetOtp(email, otp);

        return new AuthResponse("OTP sent successfully", null);
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        boolean validOtp = otpService.verifyOtp(email, otp);
        if (!validOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        return new AuthResponse("OTP verified successfully", null);
    }

    public AuthResponse resetPassword(String email, ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new AuthResponse("Password updated successfully", user.getRole());
    }

    public User completeGoogleRegistration(String email, String fullName, String avatarUrl, String role) {
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            throw new IllegalArgumentException("Role must be STUDENT or TEACHER");
        }

        String normalizedEmail = email.trim().toLowerCase();

        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            return existingUser;
        }

        String displayName = fullName;
        if (displayName == null || displayName.isBlank()) {
            displayName = normalizedEmail;
        }

        User user = User.builder()
                .fullName(displayName.trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode("GOOGLE_LOGIN_" + UUID.randomUUID()))
                .role(role)
                .status("ACTIVE")
                .avatarUrl(avatarUrl)
                .build();

        return userRepository.save(user);
    }
}
