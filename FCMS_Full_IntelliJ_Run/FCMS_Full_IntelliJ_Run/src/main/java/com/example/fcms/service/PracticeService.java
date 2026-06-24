package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PracticeService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;
    private final UserRepository userRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final FileTextExtractionService fileTextExtractionService;
    private final PracticeQuestionChatRepository practiceQuestionChatRepository;

    public PracticeService(PracticeSessionRepository practiceSessionRepository,
                           PracticeQuestionRepository practiceQuestionRepository,
                           ContentResourceRepository contentResourceRepository,
                           EnrollmentRepository enrollmentRepository,
                           LearningProgressLogRepository learningProgressLogRepository,
                           UserRepository userRepository,
                           LearningNodeRepository learningNodeRepository,
                           FileTextExtractionService fileTextExtractionService,
                           PracticeQuestionChatRepository practiceQuestionChatRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.userRepository = userRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.fileTextExtractionService = fileTextExtractionService;
        this.practiceQuestionChatRepository = practiceQuestionChatRepository;
    }

    private boolean isStudentEnrolledInContentClass(Long studentId, ContentResource resource) {
        Long classId = resource.getLearningNode().getClassRoom().getClassId();
        return enrollmentRepository.findByClassRoom_ClassIdAndStudent_UserId(classId, studentId)
                .map(e -> "ACTIVE".equals(e.getStatus()))
                .orElse(false);
    }

    public List<PracticeSession> getSessionsForStudent(Long studentId) {
        return practiceSessionRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId);
    }

    public Optional<PracticeSession> getSession(Long studentId, Long sessionId) {
        Optional<PracticeSession> sessionOpt = practiceSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            PracticeSession session = sessionOpt.get();
            if (session.getStudent().getUserId().equals(studentId)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public List<PracticeQuestion> getQuestionsForSession(Long sessionId) {
        return practiceQuestionRepository.findByPracticeSession_PracticeSessionId(sessionId);
    }

    @Transactional
    public PracticeSession generatePracticeSession(
            Long studentId,
            Long contentId,
            MultipartFile uploadedFile,
            String pastedText,
            String sourceUrl,
            String customPrompt,
            String difficulty,
            String questionType,
            Integer numberOfQuestions
    ) {
        // Validate that at least one source is provided
        boolean hasSource = (contentId != null)
                || (uploadedFile != null && !uploadedFile.isEmpty())
                || (pastedText != null && !pastedText.trim().isEmpty())
                || (sourceUrl != null && !sourceUrl.trim().isEmpty())
                || (customPrompt != null && !customPrompt.trim().isEmpty());
        if (!hasSource) {
            throw new IllegalArgumentException("Please provide at least one study material source (file, text, URL, selected content) or custom prompt instructions.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        String sourceType = "CUSTOM_PROMPT";
        String sourceTitle = "AI Prompt Instructions";
        String sourceText = "";
        String uploadedFilePath = null;
        String originalFileName = null;
        ContentResource resource = null;

        // Process source input
        if (uploadedFile != null && !uploadedFile.isEmpty()) {
            sourceType = "UPLOADED_FILE";
            try {
                String rawOriginalName = uploadedFile.getOriginalFilename();
                originalFileName = org.springframework.util.StringUtils.cleanPath(rawOriginalName != null ? rawOriginalName : "UploadedFile");
                sourceTitle = originalFileName;
                
                // Extract text
                sourceText = fileTextExtractionService.extractText(uploadedFile);

                // Use project working directory as base for absolute upload path
                java.nio.file.Path uploadDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "uploads", "ai-practice");
                java.nio.file.Files.createDirectories(uploadDir);

                String extension = getFileExtension(originalFileName);
                String savedFileName = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID() + extension;
                java.nio.file.Path targetPath = uploadDir.resolve(savedFileName);

                java.nio.file.Files.copy(uploadedFile.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                uploadedFilePath = targetPath.toAbsolutePath().toString().replace("\\", "/");
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not save uploaded file. Please try again or paste the lesson text instead.", e);
            }
        } else if (pastedText != null && !pastedText.trim().isEmpty()) {
            sourceType = "PASTED_TEXT";
            sourceTitle = "Pasted Lesson Text";
            sourceText = pastedText.trim();
        } else if (sourceUrl != null && !sourceUrl.trim().isEmpty()) {
            sourceType = "SOURCE_URL";
            sourceTitle = sourceUrl;
            sourceText = "Web resource: " + sourceUrl;
        } else if (contentId != null) {
            sourceType = "SELECTED_CONTENT";
            resource = contentResourceRepository.findById(contentId)
                    .orElseThrow(() -> new IllegalArgumentException("Content resource not found."));
            
            if (!isStudentEnrolledInContentClass(studentId, resource)) {
                throw new IllegalArgumentException("Student is not enrolled in the class for this content.");
            }
            
            sourceTitle = resource.getTitle();
            sourceText = resource.getDescription() != null ? resource.getDescription() : resource.getTitle();
        } else if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            sourceType = "CUSTOM_PROMPT";
            sourceTitle = "Custom Prompt Instructions";
            sourceText = customPrompt;
        }

        // Set default practice parameters
        int finalCount = 5;
        String finalDifficulty = "MEDIUM";
        String finalQuestionType = "MCQ";
        String focusTopic = null;
        boolean shortestCorrect = false;

        // Parse custom instructions
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            String cleanPrompt = customPrompt.toLowerCase();

            // Parse count
            if (cleanPrompt.contains("5 questions") || cleanPrompt.contains("create 5") || cleanPrompt.contains("tạo 5")) {
                finalCount = 5;
            } else {
                java.util.regex.Pattern pCount = java.util.regex.Pattern.compile("\\b(10|[1-9])\\b");
                java.util.regex.Matcher mCount = pCount.matcher(cleanPrompt);
                if (mCount.find()) {
                    finalCount = Integer.parseInt(mCount.group(1));
                } else {
                    if (numberOfQuestions != null) {
                        finalCount = numberOfQuestions;
                    }
                }
            }

            // Parse difficulty
            if (cleanPrompt.contains("easy") || cleanPrompt.contains("dễ")) {
                finalDifficulty = "EASY";
            } else if (cleanPrompt.contains("hard") || cleanPrompt.contains("khó")) {
                finalDifficulty = "HARD";
            } else if (cleanPrompt.contains("medium") || cleanPrompt.contains("trung bình")) {
                finalDifficulty = "MEDIUM";
            } else {
                if (difficulty != null && !difficulty.trim().isEmpty()) {
                    finalDifficulty = difficulty;
                }
            }

            // Parse question type
            if (cleanPrompt.contains("true/false") || cleanPrompt.contains("đúng sai")) {
                finalQuestionType = "TRUE_FALSE";
            } else if (cleanPrompt.contains("multiple choice") || cleanPrompt.contains("trắc nghiệm")) {
                finalQuestionType = "MCQ";
            } else if (cleanPrompt.contains("short answer") || cleanPrompt.contains("câu ngắn")) {
                finalQuestionType = "SHORT_ANSWER";
            } else {
                if (questionType != null && !questionType.trim().isEmpty()) {
                    finalQuestionType = questionType;
                }
            }

            // Parse focus topic
            String[] focusKeywords = {"focus on", "tập trung vào", "về phần"};
            for (String kw : focusKeywords) {
                int idx = cleanPrompt.indexOf(kw);
                if (idx != -1) {
                    String sub = customPrompt.substring(idx + kw.length()).trim();
                    int endIdx = sub.indexOf('.');
                    if (endIdx == -1) endIdx = sub.indexOf(',');
                    if (endIdx == -1) endIdx = Math.min(sub.length(), 40);
                    if (endIdx > 0) {
                        focusTopic = sub.substring(0, endIdx).trim();
                    }
                    break;
                }
            }

            // Parse answer styles
            if (cleanPrompt.contains("shortest answer") || cleanPrompt.contains("đáp án ngắn nhất")) {
                shortestCorrect = true;
            }
        } else {
            // Apply defaults if no prompt is provided
            if (numberOfQuestions != null) finalCount = numberOfQuestions;
            if (difficulty != null) finalDifficulty = difficulty;
            if (questionType != null) finalQuestionType = questionType;
        }

        // Limit count range
        if (finalCount < 1) finalCount = 1;
        if (finalCount > 10) finalCount = 10;

        // Auto-detect focus topic if not set
        if (focusTopic == null) {
            String combinedText = (sourceTitle + " " + sourceText).toLowerCase();
            if (combinedText.contains("constructivism") || combinedText.contains("constructivist") || combinedText.contains("kiến tạo")) {
                focusTopic = "social constructivism";
            } else if (combinedText.contains("flutter") || combinedText.contains("dart") || combinedText.contains("mobile") || combinedText.contains("widget")) {
                focusTopic = "Flutter";
            } else if (combinedText.contains("use case") || combinedText.contains("analysis model") || combinedText.contains("requirement") || combinedText.contains("software")) {
                focusTopic = "Use Case Diagrams";
            }
        }

        // Build and save session
        PracticeSession session = PracticeSession.builder()
                .student(student)
                .content(resource)
                .sourceType(sourceType)
                .sourceTitle(sourceTitle)
                .sourceText(sourceText)
                .sourceUrl(sourceUrl)
                .uploadedFilePath(uploadedFilePath)
                .originalFileName(originalFileName)
                .customPrompt(customPrompt)
                .difficulty(finalDifficulty.toUpperCase())
                .questionType(finalQuestionType.toUpperCase())
                .totalQuestions(finalCount)
                .correctAnswers(0)
                .build();

        session = practiceSessionRepository.save(session);

        String displayTopic = focusTopic != null ? focusTopic : sourceTitle;

        // Generate questions
        for (int i = 0; i < finalCount; i++) {
            GeneratedQuestionData data = generateQuestion(displayTopic, finalQuestionType, i, shortestCorrect);
            
            PracticeQuestion question = PracticeQuestion.builder()
                    .practiceSession(session)
                    .questionContent(data.questionContent)
                    .questionType(data.questionType.toUpperCase())
                    .optionA(data.optionA)
                    .optionB(data.optionB)
                    .optionC(data.optionC)
                    .optionD(data.optionD)
                    .correctAnswer(data.correctAnswer)
                    .explanation(data.explanation)
                    .build();

            practiceQuestionRepository.save(question);
        }

        // Save progress log under enrolled classroom if available
        LearningNode progressNode = null;
        if (resource != null) {
            progressNode = resource.getLearningNode();
        } else {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent_UserIdAndStatus(studentId, "ACTIVE");
            if (!enrollments.isEmpty()) {
                ClassRoom cr = enrollments.get(0).getClassRoom();
                List<LearningNode> nodes = learningNodeRepository.findByClassRoom_ClassIdAndVisibleTrueOrderByOrderIndexAsc(cr.getClassId());
                if (!nodes.isEmpty()) {
                    progressNode = nodes.get(0);
                }
            }
        }

        if (progressNode != null) {
            LearningProgressLog log = LearningProgressLog.builder()
                    .student(student)
                    .classRoom(progressNode.getClassRoom())
                    .learningNode(progressNode)
                    .activityType("TAKE_PRACTICE")
                    .progressPercent(BigDecimal.valueOf(100.00))
                    .build();
            learningProgressLogRepository.save(log);
        }

        return session;
    }

    private GeneratedQuestionData generateQuestion(String topic, String questionType, int index, boolean shortestCorrect) {
        GeneratedQuestionData data = new GeneratedQuestionData();
        data.questionType = questionType;

        if ("MCQ".equalsIgnoreCase(questionType)) {
            data.correctAnswer = "A";
            switch (index % 5) {
                case 0:
                    data.questionContent = "Based on the material, what is the main idea of " + topic + "?";
                    data.optionA = "The core concept of " + topic + ".";
                    data.optionB = "An unrelated secondary detail that does not represent the main points of the course syllabus.";
                    data.optionC = "A generic placeholder concept with no relevance to the actual material or slide contents.";
                    data.optionD = "None of the above choices provide an accurate description of the topic discussed in the slides.";
                    data.explanation = "This checks the key idea from the uploaded or pasted lesson material.";
                    break;
                case 1:
                    data.questionContent = "Which of the following is a primary objective when studying " + topic + "?";
                    data.optionA = "To understand fundamental principles of " + topic + ".";
                    data.optionB = "To memorize trivial syntax definitions without understanding their practical application in software development projects.";
                    data.optionC = "To ignore all pre-class preparation activities entirely and skip reading details on slides.";
                    data.optionD = "To copy solutions directly from external online forums or other classmate notebooks.";
                    data.explanation = "Understanding fundamental principles is key to mastering the topic.";
                    break;
                case 2:
                    data.questionContent = "To effectively apply " + topic + " in practice, a student should:";
                    data.optionA = "Conduct hands-on exercise with " + topic + ".";
                    data.optionB = "Delay all self-study tasks until the night before the final course examination period.";
                    data.optionC = "Only read the title of the slides and skip reading details or attempting mock practices.";
                    data.optionD = "Assume that the material has no connection to the group project work and ignore class lectures.";
                    data.explanation = "Hands-on practice is the most effective way to internalize active learning concepts.";
                    break;
                case 3:
                    data.questionContent = "What role does " + topic + " play in the overall course syllabus?";
                    data.optionA = "It forms a core building block.";
                    data.optionB = "It acts as a completely optional elective that is never assessed or discussed in exams.";
                    data.optionC = "It serves only as historical background with no relevance to current software development techniques.";
                    data.optionD = "It represents administrative guidelines for course enrollment and grading policies.";
                    data.explanation = "Each topic node on the learning path represents an essential building block.";
                    break;
                default:
                    data.questionContent = "How does the material describe the scope of " + topic + "?";
                    data.optionA = "Comprehensive and fundamental.";
                    data.optionB = "Extremely narrow with no connection to other software development lifecycle phases or designs.";
                    data.optionC = "Purely administrative with no technical content or practical implementation code.";
                    data.optionD = "Undocumented and completely left to the student's imagination with no resources.";
                    data.explanation = "Pre-class materials aim to provide a comprehensive baseline of fundamental concepts.";
                    break;
            }

            // Apply shortestCorrect constraint if requested
            if (shortestCorrect) {
                data.optionA = shortenString(data.optionA, 15);
                data.optionB = lengthenString(data.optionB, 65);
                data.optionC = lengthenString(data.optionC, 65);
                data.optionD = lengthenString(data.optionD, 65);
            }

        } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
            switch (index % 5) {
                case 0:
                    data.questionContent = "The material mainly discusses " + topic + ".";
                    data.correctAnswer = "TRUE";
                    data.explanation = "This statement reflects the main learning content.";
                    break;
                case 1:
                    data.questionContent = "Studying " + topic + " is completely optional and has no impact on classroom active discussions.";
                    data.correctAnswer = "FALSE";
                    data.explanation = "Active learning preparation directly impacts the depth of classroom collaboration.";
                    break;
                case 2:
                    data.questionContent = "The pre-class reading material for " + topic + " is designed to build foundational knowledge.";
                    data.correctAnswer = "TRUE";
                    data.explanation = "Pre-class reading builds the base concepts for active lectures.";
                    break;
                case 3:
                    data.questionContent = "Students should ignore the " + topic + " material until final exam week to maximize retention.";
                    data.correctAnswer = "FALSE";
                    data.explanation = "Flipped classroom relies on studying materials before each class session.";
                    break;
                default:
                    data.questionContent = "Collaborative discussion on " + topic + " in class enhances conceptual understanding.";
                    data.correctAnswer = "TRUE";
                    data.explanation = "Interactivity and peer debate clarify complex concepts.";
                    break;
            }
        } else { // SHORT_ANSWER
            switch (index % 5) {
                case 0:
                    data.questionContent = "Briefly explain the key idea of " + topic + ".";
                    data.correctAnswer = "Student should mention the main concept from the material.";
                    data.explanation = "This helps students review the lesson before class.";
                    break;
                case 1:
                    data.questionContent = "List one primary benefit of learning " + topic + ".";
                    data.correctAnswer = "Gaining foundational knowledge and preparing for hands-on application in the classroom.";
                    data.explanation = "Flipped classroom pre-learning paves the way for advanced classroom exercises.";
                    break;
                case 2:
                    data.questionContent = "How can you apply " + topic + " in your group projects?";
                    data.correctAnswer = "By integrating its principles and methodologies into project requirements and implementations.";
                    data.explanation = "Connecting concepts to projects reinforces learning objectives.";
                    break;
                case 3:
                    data.questionContent = "What is the most challenging aspect of " + topic + " according to the lesson?";
                    data.correctAnswer = "Synthesizing the core elements and applying them to real-world software design choices.";
                    data.explanation = "Higher levels of learning require analysis and synthesis of concepts.";
                    break;
                default:
                    data.questionContent = "Summarize the pre-class reading expectations for " + topic + ".";
                    data.correctAnswer = "Reviewing all slides, watching relevant videos, and attempting self-study practices.";
                    data.explanation = "Consistent pre-class preparation is crucial for active classroom involvement.";
                    break;
            }
        }

        return data;
    }

    private String shortenString(String s, int maxLen) {
        if (s == null) return "Core concept.";
        if (s.length() > maxLen) {
            String sub = s.substring(0, maxLen).trim();
            return sub.endsWith(".") ? sub : sub + ".";
        }
        return s;
    }

    private String lengthenString(String s, int minLen) {
        if (s == null) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < minLen) {
            sb.append(" This option serves as a verbose distractor to provide comprehensive choice alternatives for learning purposes.");
        }
        return sb.toString();
    }

    @Transactional
    public PracticeSession submitAnswers(Long studentId, Long sessionId, Map<Long, String> answers) {
        PracticeSession session = getSession(studentId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or does not belong to student."));

        List<PracticeQuestion> questions = getQuestionsForSession(sessionId);
        int correctCount = 0;

        for (PracticeQuestion q : questions) {
            String ans = answers.get(q.getPracticeQuestionId());
            if (ans != null) {
                ans = ans.trim();
            } else {
                ans = "";
            }

            q.setStudentAnswer(ans);
            boolean correct = false;

            if ("MCQ".equalsIgnoreCase(q.getQuestionType())) {
                correct = q.getCorrectAnswer().equalsIgnoreCase(ans);
            } else if ("TRUE_FALSE".equalsIgnoreCase(q.getQuestionType())) {
                correct = q.getCorrectAnswer().equalsIgnoreCase(ans);
            } else { // SHORT_ANSWER
                correct = !ans.isEmpty() && ans.length() >= 5;
            }

            q.setIsCorrect(correct);
            if (correct) {
                correctCount++;
            }
            practiceQuestionRepository.save(q);
        }

        session.setCorrectAnswers(correctCount);
        return practiceSessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long studentId) {
        PracticeSession session = practiceSessionRepository.findByPracticeSessionIdAndStudent_UserId(sessionId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Practice session not found or access denied."));

        // Delete chats first
        practiceQuestionChatRepository.deleteByPracticeSession_PracticeSessionId(sessionId);

        // Delete questions next
        practiceQuestionRepository.deleteByPracticeSession_PracticeSessionId(sessionId);

        // Delete uploaded file if exists
        String uploadedFilePath = session.getUploadedFilePath();
        if (uploadedFilePath != null && !uploadedFilePath.trim().isEmpty()) {
            try {
                java.nio.file.Path filePath = java.nio.file.Paths.get(uploadedFilePath);
                java.nio.file.Files.deleteIfExists(filePath);
            } catch (Exception ignored) {}
        }

        // Delete session
        practiceSessionRepository.delete(session);
    }

    @Transactional
    public void clearHistory(Long studentId) {
        List<PracticeSession> sessions = practiceSessionRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId);
        for (PracticeSession session : sessions) {
            // Delete chats
            practiceQuestionChatRepository.deleteByPracticeSession_PracticeSessionId(session.getPracticeSessionId());

            // Delete questions
            practiceQuestionRepository.deleteByPracticeSession_PracticeSessionId(session.getPracticeSessionId());

            // Delete file
            String uploadedFilePath = session.getUploadedFilePath();
            if (uploadedFilePath != null && !uploadedFilePath.trim().isEmpty()) {
                try {
                    java.nio.file.Path filePath = java.nio.file.Paths.get(uploadedFilePath);
                    java.nio.file.Files.deleteIfExists(filePath);
                } catch (Exception ignored) {}
            }

            // Delete session
            practiceSessionRepository.delete(session);
        }
    }

    @Transactional
    public PracticeQuestion answerQuestion(Long sessionId, Long questionId, Long studentId, String studentAnswer) {
        PracticeSession session = getSession(studentId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or access denied."));

        PracticeQuestion question = practiceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        if (!question.getPracticeSession().getPracticeSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Question does not belong to this session.");
        }

        String ans = studentAnswer != null ? studentAnswer.trim() : "";
        question.setStudentAnswer(ans);

        boolean correct = false;
        if ("MCQ".equalsIgnoreCase(question.getQuestionType())) {
            correct = question.getCorrectAnswer().trim().equalsIgnoreCase(ans);
        } else if ("TRUE_FALSE".equalsIgnoreCase(question.getQuestionType())) {
            correct = question.getCorrectAnswer().trim().equalsIgnoreCase(ans);
        } else { // SHORT_ANSWER
            correct = !ans.isEmpty() && ans.length() >= 5;
        }

        question.setIsCorrect(correct);
        question.setAnsweredAt(java.time.LocalDateTime.now());
        practiceQuestionRepository.save(question);

        // Update status to IN_PROGRESS if it was GENERATED
        if ("GENERATED".equalsIgnoreCase(session.getStatus())) {
            session.setStatus("IN_PROGRESS");
        }

        // Recalculate session correct count
        List<PracticeQuestion> questions = getQuestionsForSession(sessionId);
        long correctCount = questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count();
        session.setCorrectAnswers((int) correctCount);
        practiceSessionRepository.save(session);

        return question;
    }

    @Transactional
    public PracticeQuestion revealQuestion(Long sessionId, Long questionId, Long studentId) {
        PracticeSession session = getSession(studentId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or access denied."));

        PracticeQuestion question = practiceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        if (!question.getPracticeSession().getPracticeSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Question does not belong to this session.");
        }

        question.setRevealed(true);
        if (question.getAnsweredAt() == null) {
            question.setAnsweredAt(java.time.LocalDateTime.now());
        }
        practiceQuestionRepository.save(question);

        // Update status to IN_PROGRESS if it was GENERATED
        if ("GENERATED".equalsIgnoreCase(session.getStatus())) {
            session.setStatus("IN_PROGRESS");
            practiceSessionRepository.save(session);
        }

        return question;
    }

    @Transactional
    public PracticeQuestionChat askAboutQuestion(Long sessionId, Long questionId, Long studentId, String followUpPrompt) {
        PracticeSession session = getSession(studentId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or access denied."));

        PracticeQuestion question = practiceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        if (!question.getPracticeSession().getPracticeSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Question does not belong to this session.");
        }

        String cleanPrompt = followUpPrompt != null ? followUpPrompt.toLowerCase() : "";
        String aiResponse;
        if (cleanPrompt.contains("why") || cleanPrompt.contains("tại sao") || cleanPrompt.contains("correct") || cleanPrompt.contains("đúng")) {
            aiResponse = "Based on the lesson material and the question context, the correct answer is indeed '" + question.getCorrectAnswer() + "'. Let me expand on the explanation: " + question.getExplanation() + " This is the most accurate choice because it directly aligns with the definitions and concepts described in " + (session.getSourceTitle() != null ? session.getSourceTitle() : "the study material") + ".";
        } else if (cleanPrompt.contains("formula") || cleanPrompt.contains("công thức") || cleanPrompt.contains("math") || cleanPrompt.contains("step")) {
            aiResponse = "Here is a step-by-step breakdown of the concepts and any underlying formulas for this question:\n" +
                    "1. Identify the input variables from the question content: '" + question.getQuestionContent() + "'\n" +
                    "2. Apply the relevant standard definitions or formulas related to " + (session.getSourceTitle() != null ? session.getSourceTitle() : "this topic") + ".\n" +
                    "3. Calculate/evaluate: The correct choice is '" + question.getCorrectAnswer() + "' because the step-by-step logic yields this result.\n" +
                    "Let me know if you need more details on any specific step!";
        } else if (cleanPrompt.contains("explain") || cleanPrompt.contains("giải thích") || cleanPrompt.contains("clear") || cleanPrompt.contains("rõ")) {
            aiResponse = "Sure, let's explain this question in more detail. The question asks: '" + question.getQuestionContent() + "'.\n" +
                    "The correct option is: " + question.getCorrectAnswer() + ".\n" +
                    "Here is the detailed breakdown: " + question.getExplanation();
        } else {
            aiResponse = "This AI Practice assistant focuses on the current lesson/question. Full general chat will be integrated later.";
        }

        PracticeQuestionChat chat = PracticeQuestionChat.builder()
                .practiceQuestion(question)
                .practiceSession(session)
                .student(session.getStudent())
                .userMessage(followUpPrompt)
                .aiResponse(aiResponse)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        return practiceQuestionChatRepository.save(chat);
    }

    public List<PracticeQuestionChat> getQuestionChats(Long questionId) {
        return practiceQuestionChatRepository.findByPracticeQuestion_PracticeQuestionIdOrderByCreatedAtAsc(questionId);
    }

    public List<PracticeQuestionChat> getChatsForSession(Long sessionId) {
        return practiceQuestionChatRepository.findByPracticeSession_PracticeSessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public PracticeSession finishSession(Long sessionId, Long studentId) {
        PracticeSession session = getSession(studentId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or access denied."));
        session.setStatus("COMPLETED");
        return practiceSessionRepository.save(session);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx == -1 ? "" : fileName.substring(dotIdx);
    }

    private static class GeneratedQuestionData {
        String questionContent;
        String questionType;
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        String correctAnswer;
        String explanation;
    }
}
