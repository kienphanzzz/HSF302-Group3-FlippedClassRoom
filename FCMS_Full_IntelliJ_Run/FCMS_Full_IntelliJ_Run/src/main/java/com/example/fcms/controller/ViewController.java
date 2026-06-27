package com.example.fcms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class ViewController {

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/select-role")
    public String selectRolePage(HttpSession session, Model model) {
        String pendingEmail = (String) session.getAttribute("pendingGoogleEmail");

        if (pendingEmail == null) {
            return "redirect:/login";
        }

        model.addAttribute("pendingEmail", pendingEmail);
        model.addAttribute("pendingName", session.getAttribute("pendingGoogleName"));
        model.addAttribute("pendingAvatar", session.getAttribute("pendingGoogleAvatar"));

        return "select-role";
    }
}
