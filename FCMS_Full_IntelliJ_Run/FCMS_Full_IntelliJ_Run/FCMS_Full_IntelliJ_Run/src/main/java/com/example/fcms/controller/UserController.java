package com.example.fcms.controller;

import com.example.fcms.dto.user.UpdateProfileRequest;
import com.example.fcms.dto.user.UserResponse;
import com.example.fcms.entity.User;
import com.example.fcms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        User currentUser = userService.getCurrentUser(currentUserId);

        model.addAttribute("title", "Profile");
        model.addAttribute("activePage", "profile");
        model.addAttribute("currentUser", currentUser);

        return "profile";
    }

    @ResponseBody
    @GetMapping("/api/users/me")
    public ResponseEntity<UserResponse> getMyProfile(HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        User currentUser = userService.getCurrentUser(currentUserId);

        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    @ResponseBody
    @PutMapping("/api/users/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpSession session
    ) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        User updatedUser = userService.updateProfile(currentUserId, request);

        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }
}
