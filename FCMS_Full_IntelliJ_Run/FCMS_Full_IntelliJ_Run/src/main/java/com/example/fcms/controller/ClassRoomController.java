package com.example.fcms.controller;

import com.example.fcms.dto.classroom.ClassRoomResponse;
import com.example.fcms.dto.classroom.CreateClassRequest;
import com.example.fcms.dto.classroom.JoinClassRequest;
import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import com.example.fcms.service.ClassRoomService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final UserRepository userRepository;

    public ClassRoomController(ClassRoomService classRoomService, UserRepository userRepository) {
        this.classRoomService = classRoomService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ClassRoomResponse> createClass(
            @Valid @RequestBody CreateClassRequest request,
            HttpSession session
    ) {
        User currentUser = getCurrentUser(session);
        ClassRoomResponse response = classRoomService.createClass(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<ClassRoomResponse> joinClass(
            @Valid @RequestBody JoinClassRequest request,
            HttpSession session
    ) {
        User currentUser = getCurrentUser(session);
        ClassRoomResponse response = classRoomService.joinClass(request, currentUser);
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser(HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");

        if (currentUserId == null) {
            throw new SecurityException("Please login first");
        }

        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new SecurityException("Please login first"));
    }
}
