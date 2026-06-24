package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.service.PracticeService;
import com.example.fcms.service.StudentLearningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentPracticeController {

    private static final Long demoStudentId = 2L;

    private final PracticeService practiceService;
    private final StudentLearningService studentLearningService;
    private final com.example.fcms.repository.UserRepository userRepository;

    public StudentPracticeController(PracticeService practiceService, 
                                     StudentLearningService studentLearningService,
                                     com.example.fcms.repository.UserRepository userRepository) {
        this.practiceService = practiceService;
        this.studentLearningService = studentLearningService;
        this.userRepository = userRepository;
    }

    @GetMapping("/ai-practice")
    public String getPracticeIndex(Model model) {
        List<ContentResource> contents = studentLearningService.getAvailableContentResources(demoStudentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("contents", contents);
        return "student/ai-practice/index";
    }

    @PostMapping("/ai-practice/generate")
    public String generatePractice(
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) MultipartFile uploadedFile,
            @RequestParam(required = false) String pastedText,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String customPrompt,
            @RequestParam(required = false, defaultValue = "MEDIUM") String difficulty,
            @RequestParam(required = false, defaultValue = "MCQ") String questionType,
            @RequestParam(required = false, defaultValue = "5") Integer numberOfQuestions,
            RedirectAttributes redirectAttributes) {
        try {
            // Basic source validation
            boolean hasSource = (contentId != null)
                    || (uploadedFile != null && !uploadedFile.isEmpty())
                    || (pastedText != null && !pastedText.trim().isEmpty())
                    || (sourceUrl != null && !sourceUrl.trim().isEmpty())
                    || (customPrompt != null && !customPrompt.trim().isEmpty());

            if (!hasSource) {
                throw new IllegalArgumentException("Please provide at least one study material source (file, text, URL, selected content) or custom prompt instructions.");
            }

            if (numberOfQuestions == null) {
                numberOfQuestions = 5;
            }
            if (numberOfQuestions < 1 || numberOfQuestions > 10) {
                throw new IllegalArgumentException("Number of questions must be between 1 and 10.");
            }

            PracticeSession session = practiceService.generatePracticeSession(
                    demoStudentId,
                    contentId,
                    uploadedFile,
                    pastedText,
                    sourceUrl,
                    customPrompt,
                    difficulty,
                    questionType,
                    numberOfQuestions
            );

            return "redirect:/student/ai-practice/" + session.getPracticeSessionId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/ai-practice";
        }
    }

    @GetMapping("/ai-practice/{sessionId}")
    public String getPracticeSession(@PathVariable Long sessionId, Model model) {
        Optional<PracticeSession> sessionOpt = practiceService.getSession(demoStudentId, sessionId);
        if (sessionOpt.isEmpty()) {
            return "redirect:/student/ai-practice";
        }

        PracticeSession session = sessionOpt.get();
        List<PracticeQuestion> questions = practiceService.getQuestionsForSession(sessionId);

        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("session", session);
        model.addAttribute("questions", questions);
        
        boolean completed = questions.stream().anyMatch(q -> q.getStudentAnswer() != null);
        model.addAttribute("completed", completed);

        return "student/ai-practice/session";
    }

    @PostMapping("/ai-practice/{sessionId}/submit")
    public String submitPracticeAnswers(@PathVariable Long sessionId,
                                        @RequestParam Map<String, String> allParams,
                                        RedirectAttributes redirectAttributes) {
        try {
            Map<Long, String> answers = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("question_")) {
                    try {
                        Long qId = Long.parseLong(entry.getKey().substring("question_".length()));
                        answers.put(qId, entry.getValue());
                    } catch (NumberFormatException ignored) {}
                }
            }

            practiceService.submitAnswers(demoStudentId, sessionId, answers);
            redirectAttributes.addFlashAttribute("successMessage", "Practice questions graded!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not submit answers.");
        }
        return "redirect:/student/ai-practice/" + sessionId;
    }

    @GetMapping("/ai-practice/history")
    public String getPracticeHistory(Model model) {
        List<PracticeSession> history = practiceService.getSessionsForStudent(demoStudentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("history", history);
        return "student/ai-practice/history";
    }
}
