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
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/student")
public class StudentPracticeController {

    private static final Long demoStudentId = 2L;

    private Long getStudentId(HttpSession session) {
        Long studentId = (Long) session.getAttribute("currentUserId");
        if (studentId == null) {
            studentId = demoStudentId;
        }
        return studentId;
    }


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
    public String getPracticeIndex(Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        List<ContentResource> contents = studentLearningService.getAvailableContentResources(studentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("contents", contents);
        model.addAttribute("activePage", "ai");
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
            RedirectAttributes redirectAttributes,
            HttpSession session) {
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

            Long studentId = getStudentId(session);
            PracticeSession sessionObj = practiceService.generatePracticeSession(
                    studentId,
                    contentId,
                    uploadedFile,
                    pastedText,
                    sourceUrl,
                    customPrompt,
                    difficulty,
                    questionType,
                    numberOfQuestions
            );

            return "redirect:/student/ai-practice/" + sessionObj.getPracticeSessionId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/ai-practice";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save practice session due to database or system error: " + e.getMessage() + ". Please make sure the SQL database migrations have been successfully run.");
            return "redirect:/student/ai-practice";
        }
    }

    @GetMapping("/ai-practice/{sessionId}")
    public String getPracticeSession(@PathVariable Long sessionId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        Optional<PracticeSession> sessionOpt = practiceService.getSession(studentId, sessionId);
        if (sessionOpt.isEmpty()) {
            return "redirect:/student/ai-practice";
        }

        PracticeSession sessionObj = sessionOpt.get();
        List<PracticeQuestion> questions = practiceService.getQuestionsForSession(sessionId);

        // Fallbacks for display variables
        if (sessionObj.getDifficulty() == null) sessionObj.setDifficulty("MEDIUM");
        if (sessionObj.getQuestionType() == null) sessionObj.setQuestionType("MCQ");
        if (sessionObj.getCustomPrompt() == null || sessionObj.getCustomPrompt().trim().isEmpty()) {
            sessionObj.setCustomPrompt("Generate practice questions from this learning material.");
        }
        if (sessionObj.getSourceTitle() == null || sessionObj.getSourceTitle().trim().isEmpty()) {
            String fallback = sessionObj.getOriginalFileName() != null ? sessionObj.getOriginalFileName() : "Uploaded material";
            sessionObj.setSourceTitle(fallback);
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
        model.addAttribute("practiceSession", sessionObj);
        model.addAttribute("questions", questions);
        model.addAttribute("answeredCount", answeredCount);
        model.addAttribute("revealedCount", revealedCount);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("accuracy", accuracy);

        boolean completed = "COMPLETED".equalsIgnoreCase(sessionObj.getStatus());
        model.addAttribute("completed", completed);
        model.addAttribute("activePage", "ai");

        return "student/ai-practice/session";
    }

    @PostMapping("/ai-practice/{sessionId}/questions/{questionId}/answer")
    public String answerSingleQuestion(@PathVariable Long sessionId,
                                       @PathVariable Long questionId,
                                       @RequestParam("studentAnswer") String studentAnswer,
                                       RedirectAttributes redirectAttributes,
                                       HttpSession session) {
        try {
            Long studentId = getStudentId(session);
            practiceService.answerQuestion(sessionId, questionId, studentId, studentAnswer);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to grade answer: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "#question-" + questionId;
    }


    @PostMapping("/ai-practice/{sessionId}/questions/{questionId}/reveal")
    public String revealSingleQuestion(@PathVariable Long sessionId,
                                       @PathVariable Long questionId,
                                       RedirectAttributes redirectAttributes,
                                       HttpSession session) {
        try {
            Long studentId = getStudentId(session);
            practiceService.revealQuestion(sessionId, questionId, studentId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reveal answer: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "#question-" + questionId;
    }


    @PostMapping("/ai-practice/{sessionId}/questions/{questionId}/ask")
    public String askAIAboutQuestion(@PathVariable Long sessionId,
                                     @PathVariable Long questionId,
                                     @RequestParam("followUpPrompt") String followUpPrompt,
                                     RedirectAttributes redirectAttributes,
                                     HttpSession session) {
        try {
            if (followUpPrompt == null || followUpPrompt.trim().isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be empty.");
            }
            Long studentId = getStudentId(session);
            practiceService.askAboutQuestion(sessionId, questionId, studentId, followUpPrompt);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to get explanation: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "#question-" + questionId;
    }


    @PostMapping("/ai-practice/{sessionId}/finish")
    public String finishPracticeSession(@PathVariable Long sessionId,
                                        RedirectAttributes redirectAttributes,
                                        HttpSession session) {
        try {
            Long studentId = getStudentId(session);
            practiceService.finishSession(sessionId, studentId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to finish practice session: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/" + sessionId + "/result";
    }


    @GetMapping("/ai-practice/{sessionId}/result")
    public String getPracticeResult(@PathVariable Long sessionId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        Optional<PracticeSession> sessionOpt = practiceService.getSession(studentId, sessionId);
        if (sessionOpt.isEmpty()) {
            return "redirect:/student/ai-practice";
        }

        PracticeSession sessionObj = sessionOpt.get();
        List<PracticeQuestion> questions = practiceService.getQuestionsForSession(sessionId);

        // Fallbacks for display variables
        if (sessionObj.getDifficulty() == null) sessionObj.setDifficulty("MEDIUM");
        if (sessionObj.getQuestionType() == null) sessionObj.setQuestionType("MCQ");
        if (sessionObj.getCustomPrompt() == null || sessionObj.getCustomPrompt().trim().isEmpty()) {
            sessionObj.setCustomPrompt("Generate practice questions from this learning material.");
        }
        if (sessionObj.getSourceTitle() == null || sessionObj.getSourceTitle().trim().isEmpty()) {
            String fallback = sessionObj.getOriginalFileName() != null ? sessionObj.getOriginalFileName() : "Uploaded material";
            sessionObj.setSourceTitle(fallback);
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
        model.addAttribute("practiceSession", sessionObj);
        model.addAttribute("questions", questions);
        model.addAttribute("answeredCount", answeredCount);
        model.addAttribute("revealedCount", revealedCount);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("accuracy", accuracy);
        model.addAttribute("activePage", "ai");

        return "student/ai-practice/result";
    }


    @PostMapping("/ai-practice/{sessionId}/submit")
    public String submitPracticeAnswers(@PathVariable Long sessionId,
                                        @RequestParam Map<String, String> allParams,
                                        RedirectAttributes redirectAttributes,
                                        HttpSession session) {
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

            Long studentId = getStudentId(session);
            practiceService.submitAnswers(studentId, sessionId, answers);
            redirectAttributes.addFlashAttribute("successMessage", "Practice questions graded!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not submit answers.");
        }
        return "redirect:/student/ai-practice/" + sessionId;
    }


    @GetMapping("/ai-practice/history")
    public String getPracticeHistory(Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        List<PracticeSession> sessions = practiceService.getSessionsForStudent(studentId);
        List<PracticeSessionHistoryDto> history = new java.util.ArrayList<>();
        for (PracticeSession s : sessions) {
            List<PracticeQuestion> questions = practiceService.getQuestionsForSession(s.getPracticeSessionId());
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
            
            // Fallbacks for display
            if (s.getDifficulty() == null) s.setDifficulty("MEDIUM");
            if (s.getQuestionType() == null) s.setQuestionType("MCQ");
            if (s.getSourceTitle() == null || s.getSourceTitle().trim().isEmpty()) {
                String fallback = s.getOriginalFileName() != null ? s.getOriginalFileName() : "Uploaded material";
                s.setSourceTitle(fallback);
            }
            if (s.getCustomPrompt() == null || s.getCustomPrompt().trim().isEmpty()) {
                s.setCustomPrompt("Generate practice questions from this learning material.");
            }

            history.add(new PracticeSessionHistoryDto(s, answeredCount, correctCount, revealedCount, accuracy));
        }


        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("history", history);
        model.addAttribute("activePage", "ai");
        return "student/ai-practice/history";
    }

    @PostMapping("/ai-practice/{sessionId}/delete")
    public String deletePracticeSession(@PathVariable Long sessionId, RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            Long studentId = getStudentId(session);
            practiceService.deleteSession(sessionId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Practice history deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete practice history: " + e.getMessage());
        }
        return "redirect:/student/ai-practice/history";
    }

    @PostMapping("/ai-practice/history/clear")
    public String clearPracticeHistory(RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            Long studentId = getStudentId(session);
            practiceService.clearHistory(studentId);
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

    public static class PracticeSessionHistoryDto {
        private final PracticeSession session;
        private final long answeredCount;
        private final long correctCount;
        private final long revealedCount;
        private final int accuracy;

        public PracticeSessionHistoryDto(PracticeSession session, long answeredCount, long correctCount, long revealedCount, int accuracy) {
            this.session = session;
            this.answeredCount = answeredCount;
            this.correctCount = correctCount;
            this.revealedCount = revealedCount;
            this.accuracy = accuracy;
        }

        public Long getPracticeSessionId() { return session.getPracticeSessionId(); }
        public String getSourceTitle() { return session.getSourceTitle(); }
        public String getSourceType() { return session.getSourceType(); }
        public String getCustomPrompt() { return session.getCustomPrompt(); }
        public String getDifficulty() { return session.getDifficulty(); }
        public String getQuestionType() { return session.getQuestionType(); }
        public Integer getTotalQuestions() { return session.getTotalQuestions(); }
        public String getStatus() { return session.getStatus(); }
        public java.time.LocalDateTime getCreatedAt() { return session.getCreatedAt(); }
        public long getAnsweredCount() { return answeredCount; }
        public long getCorrectCount() { return correctCount; }
        public long getRevealedCount() { return revealedCount; }
        public int getAccuracy() { return accuracy; }
    }
}
