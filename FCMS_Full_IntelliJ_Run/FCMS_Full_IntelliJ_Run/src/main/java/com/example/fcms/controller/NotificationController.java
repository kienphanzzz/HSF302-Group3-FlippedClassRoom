package com.example.fcms.controller;

import com.example.fcms.entity.Notification;
import com.example.fcms.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String showNotifications(HttpSession session, Model model) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        if (currentUserId == null) {
            return "redirect:/login";
        }
        
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUserId);
        model.addAttribute("notifications", notifications);
        model.addAttribute("activePage", "notifications");
        
        return "notifications";
    }

    @PostMapping("/notifications/mark-read")
    public String markAllRead(HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        if (currentUserId != null) {
            notificationService.markAllAsRead(currentUserId);
        }
        return "redirect:/notifications";
    }
}
