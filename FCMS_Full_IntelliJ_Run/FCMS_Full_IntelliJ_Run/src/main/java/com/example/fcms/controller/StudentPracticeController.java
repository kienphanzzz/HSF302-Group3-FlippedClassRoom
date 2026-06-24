package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.service.PracticeService;
import com.example.fcms.service.StudentLearningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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
            @RequestParam(value = "contentId", required = false) Long contentId,
            @RequestParam(value = "uploadedFile", required = false) MultipartFile uploadedFile,
            @RequestParam(value = "pastedText", required = false) String pastedText,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl,
            @RequestParam(value = "customPrompt", required = false) String customPrompt,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "questionType", required = false) String questionType,
            @RequestParam(value = "numberOfQuestions", required = false) Integer numberOfQuestions,
            RedirectAttributes redirectAttributes) {
        try {
            // Validation: Request is valid if at least one of these exists
            boolean hasSource = (contentId != null)
                    || (uploadedFile != null && !uploadedFile.isEmpty())
                    || (pastedText != null && !pastedText.trim().isEmpty())
                    || (sourceUrl != null && !sourceUrl.trim().isEmpty())
                    || (customPrompt != null && !customPrompt.trim().isEmpty());

            if (!hasSource) {
                throw new IllegalArgumentException("Please provide at least one study material source (file, text, URL, selected content) or custom prompt instructions.");
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
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save practice session due to database or system error: " + e.getMessage() + ". Please make sure the SQL database migrations have been successfully run.");
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

        // Fallbacks for display variables
        if (session.getDifficulty() == null) session.setDifficulty("MEDIUM");
        if (session.getQuestionType() == null) session.setQuestionType("MCQ");
        if (session.getCustomPrompt() == null || session.getCustomPrompt().trim().isEmpty()) {
            session.setCustomPrompt("Generate practice questions from this learning material.");
        }
        if (session.getSourceTitle() == null || session.getSourceTitle().trim().isEmpty()) {
            String fallback = session.getOriginalFileName() != null ? session.getOriginalFileName() : "Uploaded material";
            session.setSourceTitle(fallback);
        }

        // Statistics
        long answeredCount = questions.stream()
                .filter(q -> q.getStudentAnswer() != null && !q.getStudentAnswer().trim().isEmpty())
                .count();
        long revealedCount = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getRevealed()))
                .count();
        long correctCount = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        int accuracy = 0;
        if (answeredCount > 0) {
            accuracy = (int) Math.round((double) correctCount * 100.0 / answeredCount);
        }

        // Fetch follow-up chat history and attach directly to each question
        List<PracticeQuestionChat> chats = practiceService.getChatsForSession(sessionId);
        Map<Long, List<PracticeQuestionChat>> chatsMap = chats.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getPracticeQuestion().getPracticeQuestionId()));
        for (PracticeQuestion q : questions) {
            q.setChats(chatsMap.getOrDefault(q.getPracticeQuestionId(), new java.util.ArrayList<>()));
        }

        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("practiceSession", session);
        model.addAttribute("questions", questions);
        model.addAttribute("answeredCount", answeredCount);
        model.addAttribute("revealedCount", revealedCount);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("accuracy", accuracy);

        boolean completed = "COMPLETED".equalsIgnoreCase(session.getStatus());
        model.addAttribute("completed", completed);

        return "student/ai-practice/session";
    }

    @PostMapping("/ai-practice/{sessionId}/questions/{questionId}/answer")
    public String answerSingleQuestion(@PathVariable Long sessionId,
                                       @PathVariable Long questionId,
                                       @RequestParam("studentAnswer") String studentAnswer,
                                       RedirectAttributes redirectAttributes) {
        try {
            practiceService.answerQuestion(sessionId, questionId, demoStudentId, studentAnswer);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to grade answer: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "#question-" + questionId;
    }

    @PostMapping("/ai-practice/{sessionId}/questions/{questionId}/reveal")
    public String revealSingleQuestion(@PathVariable Long sessionId,
                                       @PathVariable Long questionId,
                                       RedirectAttributes redirectAttributes) {
        try {
            practiceService.revealQuestion(sessionId, questionId, demoStudentId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reveal answer: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "#question-" + questionId;
    }

    @PostMapping("/ai-practice/{sessionId}/questions/{questionId}/ask")
    public String askAIAboutQuestion(@PathVariable Long sessionId,
                                     @PathVariable Long questionId,
                                     @RequestParam("followUpPrompt") String followUpPrompt,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (followUpPrompt == null || followUpPrompt.trim().isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be empty.");
            }
            practiceService.askAboutQuestion(sessionId, questionId, demoStudentId, followUpPrompt);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to get explanation: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "#question-" + questionId;
    }

    @PostMapping("/ai-practice/{sessionId}/finish")
    public String finishPracticeSession(@PathVariable Long sessionId,
                                        RedirectAttributes redirectAttributes) {
        try {
            practiceService.finishSession(sessionId, demoStudentId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to finish practice session: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "/result";
    }

    @GetMapping("/ai-practice/{sessionId}/result")
    public String getPracticeResult(@PathVariable Long sessionId, Model model) {
        Optional<PracticeSession> sessionOpt = practiceService.getSession(demoStudentId, sessionId);
        if (sessionOpt.isEmpty()) {
            return "redirect:/student/ai-practice";
        }

        PracticeSession session = sessionOpt.get();
        List<PracticeQuestion> questions = practiceService.getQuestionsForSession(sessionId);

        // Fallbacks for display variables
        if (session.getDifficulty() == null) session.setDifficulty("MEDIUM");
        if (session.getQuestionType() == null) session.setQuestionType("MCQ");
        if (session.getCustomPrompt() == null || session.getCustomPrompt().trim().isEmpty()) {
            session.setCustomPrompt("Generate practice questions from this learning material.");
        }
        if (session.getSourceTitle() == null || session.getSourceTitle().trim().isEmpty()) {
            String fallback = session.getOriginalFileName() != null ? session.getOriginalFileName() : "Uploaded material";
            session.setSourceTitle(fallback);
        }

        long answeredCount = questions.stream()
                .filter(q -> q.getStudentAnswer() != null && !q.getStudentAnswer().trim().isEmpty())
                .count();
        long revealedCount = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getRevealed()))
                .count();
        long correctCount = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        int accuracy = 0;
        if (answeredCount > 0) {
            accuracy = (int) Math.round((double) correctCount * 100.0 / answeredCount);
        }

        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("practiceSession", session);
        model.addAttribute("questions", questions);
        model.addAttribute("answeredCount", answeredCount);
        model.addAttribute("revealedCount", revealedCount);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("accuracy", accuracy);

        return "student/ai-practice/result";
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

    @PostMapping("/ai-practice/{sessionId}/delete")
    public String deletePracticeSession(@PathVariable Long sessionId, RedirectAttributes redirectAttributes) {
        try {
            practiceService.deleteSession(sessionId, demoStudentId);
            redirectAttributes.addFlashAttribute("successMessage", "Practice history deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete practice history: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/history";
    }

    @PostMapping("/ai-practice/history/clear")
    public String clearPracticeHistory(RedirectAttributes redirectAttributes) {
        try {
            practiceService.clearHistory(demoStudentId);
            redirectAttributes.addFlashAttribute("successMessage", "All practice history has been cleared.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not clear practice history: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/history";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Uploaded file is too large. Please upload a file under 50MB or paste the lesson text instead.");
        return "redirect:/student/ai-practice";
    }
}
