package com.example.fcms.controller;

import com.example.fcms.service.StudentProgressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentProgressController {

    // TODO: Use logged-in user instead of demoStudentId when authentication is integrated
    private static final Long demoStudentId = 2L;

    private final StudentProgressService studentProgressService;

    public StudentProgressController(StudentProgressService studentProgressService) {
        this.studentProgressService = studentProgressService;
    }

    @GetMapping("/progress")
    public String getProgress(Model model) {
        Map<String, Object> summary = studentProgressService.getProgressSummary(demoStudentId);
        
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAllAttributes(summary);
        
        return "student/progress";
    }
}
