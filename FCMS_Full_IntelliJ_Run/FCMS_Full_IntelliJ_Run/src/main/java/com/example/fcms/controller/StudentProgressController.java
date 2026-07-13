package com.example.fcms.controller;

import com.example.fcms.service.StudentProgressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/student")
public class StudentProgressController {

    private static final Long demoStudentId = 2L;

    private Long getStudentId(HttpSession session) {
        Long studentId = (Long) session.getAttribute("currentUserId");
        if (studentId == null) {
            studentId = demoStudentId;
        }
        return studentId;
    }


    private final StudentProgressService studentProgressService;

    public StudentProgressController(StudentProgressService studentProgressService) {
        this.studentProgressService = studentProgressService;
    }

    @GetMapping("/progress")
    public String getProgress(Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        Map<String, Object> summary = studentProgressService.getProgressSummary(studentId);
        
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAllAttributes(summary);
        
        return "student/progress";
    }

}
