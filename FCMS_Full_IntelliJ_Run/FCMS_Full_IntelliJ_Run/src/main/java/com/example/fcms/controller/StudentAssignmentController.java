package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.service.StudentAssignmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/student")
public class StudentAssignmentController {

    private static final Long demoStudentId = 2L;

    private Long getStudentId(HttpSession session) {
        Long studentId = (Long) session.getAttribute("currentUserId");
        if (studentId == null) {
            studentId = demoStudentId;
        }
        return studentId;
    }


    private final StudentAssignmentService studentAssignmentService;

    public StudentAssignmentController(StudentAssignmentService studentAssignmentService) {
        this.studentAssignmentService = studentAssignmentService;
    }

    @GetMapping("/assignments")
    public String getAssignments(Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        List<Assignment> assignments = studentAssignmentService.getAssignmentsForStudent(studentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("assignments", assignments);
        model.addAttribute("activePage", "assignments");
        return "student/assignments/list";
    }


    @GetMapping("/assignments/{assignmentId}")
    public String getAssignmentDetail(@PathVariable Long assignmentId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        Optional<Assignment> assignmentOpt = studentAssignmentService.getAssignment(studentId, assignmentId);
        if (assignmentOpt.isEmpty()) {
            return "redirect:/student/assignments";
        }

        Assignment assignment = assignmentOpt.get();
        Optional<Submission> submissionOpt = studentAssignmentService.getSubmissionForAssignment(studentId, assignmentId);

        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submissionOpt.orElse(null));
        model.addAttribute("activePage", "assignments");
        return "student/assignments/detail";
    }


    @PostMapping("/assignments/{assignmentId}/submit")
    public String submitAssignment(@PathVariable Long assignmentId,
                                   @RequestParam String answerText,
                                   @RequestParam(required = false) String filePath,
                                   @RequestParam(required = false) String originalFileName,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        try {
            // Default file path placeholder if empty
            String finalFilePath = (filePath == null || filePath.trim().isEmpty()) ? "uploads/placeholder.pdf" : filePath;
            String finalOriginalFileName = (originalFileName == null || originalFileName.trim().isEmpty()) ? "placeholder.pdf" : originalFileName;

            Long studentId = getStudentId(session);
            studentAssignmentService.submitAssignment(studentId, assignmentId, answerText, finalFilePath, finalOriginalFileName);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment submitted successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred during submission.");
        }
        return "redirect:/student/assignments/" + assignmentId;
    }


    @GetMapping("/submissions")
    public String getSubmissions(Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        List<Submission> submissions = studentAssignmentService.getSubmissionsForStudent(studentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("submissions", submissions);
        model.addAttribute("activePage", "assignments");
        return "student/submissions/list";
    }


    @GetMapping("/submissions/{submissionId}")
    public String getSubmissionDetail(@PathVariable Long submissionId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        Optional<Submission> submissionOpt = studentAssignmentService.getSubmission(studentId, submissionId);
        if (submissionOpt.isEmpty()) {
            return "redirect:/student/submissions";
        }

        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("submission", submissionOpt.get());
        model.addAttribute("activePage", "assignments");
        return "student/submissions/detail";
    }

}
