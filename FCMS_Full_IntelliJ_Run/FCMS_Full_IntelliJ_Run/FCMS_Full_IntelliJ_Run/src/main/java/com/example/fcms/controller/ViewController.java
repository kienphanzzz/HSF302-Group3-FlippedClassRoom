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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ─── Student Portal Redirects ──────────────────────────────────────────
    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "redirect:/dashboard";
    }

    @GetMapping("/student/courses")
    public String studentCourses() {
        return "redirect:/student/learning-path";
    }

    @GetMapping("/student/assignment")
    public String assignmentSubmissionRedirect() {
        return "redirect:/student/assignments";
    }

    @GetMapping("/student/ai-assistant")
    public String aiAssistantRedirect() {
        return "redirect:/student/ai-practice";
    }

    // ─── Teacher Portal Redirects ──────────────────────────────────────────
    @GetMapping("/teacher/classes")
    public String teacherClasses() {
        return "redirect:/dashboard";
    }

    @GetMapping("/teacher/question-bank")
    public String questionBank() {
        return "redirect:/dashboard";
    }

    @GetMapping("/teacher/progress")
    public String teacherProgress() {
        return "redirect:/dashboard";
    }

    @GetMapping("/teacher/dashboard")
    public String teacherDashboardRedirect() {
        return "redirect:/dashboard";
    }
}

