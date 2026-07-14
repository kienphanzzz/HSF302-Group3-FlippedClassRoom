package com.example.fcms.controller;

import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    public GlobalControllerAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                model.addAttribute("currentUser", currentUser);
            }
        } else {
            // Fallback for demo student to prevent UI crash
            User demoStudent = userRepository.findByEmail("demo-student@fcms.local").orElse(null);
            if (demoStudent != null) {
                model.addAttribute("currentUser", demoStudent);
            }
        }
    }
}
