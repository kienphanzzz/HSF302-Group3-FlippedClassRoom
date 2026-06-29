package com.example.fcms.controller;

import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import com.example.fcms.service.ClassRoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final ClassRoomService classRoomService;

    public DashboardController(UserRepository userRepository, ClassRoomService classRoomService) {
        this.userRepository = userRepository;
        this.classRoomService = classRoomService;
    }

    @GetMapping("/dashboard")
    public String dashboardPage(HttpSession session, Model model) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");

        if (currentUserId == null || currentUserRole == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }

        model.addAttribute("title", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("currentUser", currentUser);
        List<?> classes;
        long totalStudents = 0;

        if ("TEACHER".equals(currentUserRole)) {
            classes = classRoomService.getTeacherClasses(currentUser);
            totalStudents = classRoomService.countTeacherStudents(currentUser);
        } else {
            classes = classRoomService.getStudentClasses(currentUser);
        }

        model.addAttribute("classCount", classes.size());
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("classes", classes);

        return "dashboard";
    }
}
