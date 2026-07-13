package com.example.fcms.config;

import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public GoogleLoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();

        String email = googleUser.getAttribute("email");
        String name = googleUser.getAttribute("name");
        String avatar = googleUser.getAttribute("picture");

        if (email == null || email.isBlank()) {
            response.sendRedirect("/login");
            return;
        }

        HttpSession session = request.getSession();
        String normalizedEmail = email.trim().toLowerCase();

        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            session.setAttribute("currentUserId", existingUser.getUserId());
            session.setAttribute("currentUserRole", existingUser.getRole());
            response.sendRedirect("/dashboard");
            return;
        }

        session.setAttribute("pendingGoogleEmail", normalizedEmail);
        session.setAttribute("pendingGoogleName", name);
        session.setAttribute("pendingGoogleAvatar", avatar);

        response.sendRedirect("/select-role");
    }
}
