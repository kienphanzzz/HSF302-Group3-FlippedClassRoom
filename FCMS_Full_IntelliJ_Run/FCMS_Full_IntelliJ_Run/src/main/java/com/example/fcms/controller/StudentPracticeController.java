package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.service.PracticeService;
import com.example.fcms.service.StudentLearningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentPracticeController {

    // TODO: Use logged-in user instead of demoStudentId when authentication is integrated
    private static final Long demoStudentId = 2L;

    private final PracticeService practiceService;
    private final StudentLearningService studentLearningService;
    private final com.example.fcms.repository.ContentResourceRepository contentResourceRepository;
    private final com.example.fcms.repository.LearningNodeRepository learningNodeRepository;
    private final com.example.fcms.repository.UserRepository userRepository;

    public StudentPracticeController(PracticeService practiceService, 
                                     StudentLearningService studentLearningService,
                                     com.example.fcms.repository.ContentResourceRepository contentResourceRepository,
                                     com.example.fcms.repository.LearningNodeRepository learningNodeRepository,
                                     com.example.fcms.repository.UserRepository userRepository) {
        this.practiceService = practiceService;
        this.studentLearningService = studentLearningService;
        this.contentResourceRepository = contentResourceRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/ai-practice")
    public String getPracticeIndex(Model model) {
        List<ContentResource> contents = studentLearningService.getAvailableContentResources(demoStudentId);
        List<LearningNode> nodes = studentLearningService.getAvailableLearningNodes(demoStudentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("contents", contents);
        model.addAttribute("nodes", nodes);
        return "student/ai-practice/index";
    }

    @PostMapping("/ai-practice/generate")
    public String generatePractice(@RequestParam String practiceMode,
                                   @RequestParam(required = false) Long contentId,
                                   @RequestParam(required = false) Long nodeId,
                                   @RequestParam(required = false) String linkUrl,
                                   @RequestParam(required = false) String customFileName,
                                   @RequestParam String difficulty,
                                   @RequestParam String questionType,
                                   @RequestParam int count,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long finalContentId = contentId;
            User student = userRepository.findById(demoStudentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

            if ("LINK".equalsIgnoreCase(practiceMode)) {
                if (linkUrl == null || linkUrl.trim().isEmpty() || nodeId == null) {
                    throw new IllegalArgumentException("Link URL and Topic Node are required for custom link practice.");
                }
                LearningNode node = learningNodeRepository.findById(nodeId)
                        .orElseThrow(() -> new IllegalArgumentException("Learning Node not found."));
                ContentResource newRes = ContentResource.builder()
                        .learningNode(node)
                        .uploadedBy(student)
                        .title("Custom Link: " + (linkUrl.length() > 30 ? linkUrl.substring(0, 30) + "..." : linkUrl))
                        .description("Custom study resource submitted by student.")
                        .contentType("EXTERNAL_LINK")
                        .externalUrl(linkUrl)
                        .visible(true)
                        .build();
                newRes = contentResourceRepository.save(newRes);
                finalContentId = newRes.getContentId();

            } else if ("FILE".equalsIgnoreCase(practiceMode)) {
                if (customFileName == null || customFileName.trim().isEmpty() || nodeId == null) {
                    throw new IllegalArgumentException("File and Topic Node are required for custom file practice.");
                }
                LearningNode node = learningNodeRepository.findById(nodeId)
                        .orElseThrow(() -> new IllegalArgumentException("Learning Node not found."));
                ContentResource newRes = ContentResource.builder()
                        .learningNode(node)
                        .uploadedBy(student)
                        .title("Custom File: " + customFileName)
                        .description("Custom file upload submitted by student.")
                        .contentType("FILE")
                        .filePath("uploads/" + customFileName)
                        .originalFileName(customFileName)
                        .visible(true)
                        .build();
                newRes = contentResourceRepository.save(newRes);
                finalContentId = newRes.getContentId();
            }

            if (finalContentId == null) {
                throw new IllegalArgumentException("No study material selected.");
            }

            PracticeSession session = practiceService.generatePracticeSession(demoStudentId, finalContentId, difficulty, questionType, count);
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
        
        // Check if any question has already been answered to determine if session is completed
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
