package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public PracticeService(PracticeSessionRepository practiceSessionRepository,
                           PracticeQuestionRepository practiceQuestionRepository,
                           ContentResourceRepository contentResourceRepository,
                           EnrollmentRepository enrollmentRepository,
                           LearningProgressLogRepository learningProgressLogRepository,
                           UserRepository userRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.userRepository = userRepository;
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
    public PracticeSession generatePracticeSession(Long studentId, Long contentId, String difficulty, String questionType, int count) {
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException("Number of questions must be between 1 and 10.");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new IllegalArgumentException("Difficulty must not be blank.");
        }
        if (questionType == null || questionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Question type must not be blank.");
        }

        ContentResource resource = contentResourceRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content resource not found."));

        if (!isStudentEnrolledInContentClass(studentId, resource)) {
            throw new IllegalArgumentException("Student is not enrolled in the class for this content.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        PracticeSession session = PracticeSession.builder()
                .student(student)
                .content(resource)
                .difficulty(difficulty.toUpperCase())
                .questionType(questionType.toUpperCase())
                .totalQuestions(count)
                .correctAnswers(0)
                .build();

        session = practiceSessionRepository.save(session);

        String title = resource.getTitle();
        List<PracticeQuestion> questions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String questionContent = "";
            String correctAnswer = "";
            String explanation = "";

            if ("MCQ".equalsIgnoreCase(questionType)) {
                switch (i % 4) {
                    case 0:
                        questionContent = "What is the main idea of '" + title + "'?\n\nA) The main concept of the selected learning material.\nB) An unrelated minor detail.\nC) A generic placeholder concept.\nD) None of the above.";
                        correctAnswer = "A";
                        explanation = "This question helps students review the key concept before class.";
                        break;
                    case 1:
                        questionContent = "Which of the following best describes the scope of '" + title + "'?\n\nA) High-level overview and fundamental concepts.\nB) Advanced implementation details only.\nC) Administrative instructions.\nD) Historical background only.";
                        correctAnswer = "A";
                        explanation = "Flipped classroom material covers foundational topics to prepare students.";
                        break;
                    case 2:
                        questionContent = "To get the most out of '" + title + "', students should:\n\nA) Read and study it carefully before class.\nB) Ignore it until the final exam.\nC) Copy notes from a classmate.\nD) Skip class after reading.";
                        correctAnswer = "A";
                        explanation = "Studying pre-class materials is essential for active classroom discussions.";
                        break;
                    default:
                        questionContent = "Under what topic does '" + title + "' fall?\n\nA) Core curriculum topics.\nB) Elective reading.\nC) Extracurricular activity.\nD) None of the above.";
                        correctAnswer = "A";
                        explanation = "This resource is mapped to the official learning path nodes.";
                        break;
                }
            } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                switch (i % 4) {
                    case 0:
                        questionContent = "The material '" + title + "' is used for pre-class learning.\n\nA) True\nB) False";
                        correctAnswer = "A";
                        explanation = "In flipped classroom, students study materials before class.";
                        break;
                    case 1:
                        questionContent = "Studying '" + title + "' is completely optional and has no effect on classroom discussion quality.\n\nA) True\nB) False";
                        correctAnswer = "B";
                        explanation = "Pre-class preparation directly impacts the depth of in-class collaboration.";
                        break;
                    case 2:
                        questionContent = "The content resource '" + title + "' is visible to enrolled students.\n\nA) True\nB) False";
                        correctAnswer = "A";
                        explanation = "All visible learning node resources are accessible by enrolled students.";
                        break;
                    default:
                        questionContent = "You should read or watch '" + title + "' only after the course has finished.\n\nA) True\nB) False";
                        correctAnswer = "B";
                        explanation = "Materials should be reviewed prior to class sessions.";
                        break;
                }
            } else { // SHORT_ANSWER
                switch (i % 4) {
                    case 0:
                        questionContent = "Briefly explain what you learned from '" + title + "'.";
                        correctAnswer = "Student should mention the main concept of the material.";
                        explanation = "This question checks understanding of the learning resource.";
                        break;
                    case 1:
                        questionContent = "State one key takeaway from '" + title + "'.";
                        correctAnswer = "Any relevant takeaway from the content.";
                        explanation = "Reflecting on takeaways helps consolidate knowledge.";
                        break;
                    case 2:
                        questionContent = "How does the material '" + title + "' relate to your course project?";
                        correctAnswer = "It provides the conceptual foundation or templates.";
                        explanation = "Applying concepts to projects is a key learning goal.";
                        break;
                    default:
                        questionContent = "Define the primary target audience for '" + title + "'.";
                        correctAnswer = "Students enrolled in this class.";
                        explanation = "Resources are tailored for the class level and objectives.";
                        break;
                }
            }

            PracticeQuestion question = PracticeQuestion.builder()
                    .practiceSession(session)
                    .questionContent(questionContent)
                    .questionType(questionType.toUpperCase())
                    .correctAnswer(correctAnswer)
                    .explanation(explanation)
                    .build();

            practiceQuestionRepository.save(question);
        }

        // Save a learning progress log with activity type TAKE_PRACTICE
        LearningProgressLog log = LearningProgressLog.builder()
                .student(student)
                .classRoom(resource.getLearningNode().getClassRoom())
                .learningNode(resource.getLearningNode())
                .activityType("TAKE_PRACTICE")
                .progressPercent(BigDecimal.valueOf(100.00))
                .build();
        learningProgressLogRepository.save(log);

        return session;
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

            if ("MCQ".equalsIgnoreCase(q.getQuestionType()) || "TRUE_FALSE".equalsIgnoreCase(q.getQuestionType())) {
                correct = q.getCorrectAnswer().equalsIgnoreCase(ans);
            } else { // SHORT_ANSWER
                correct = !ans.isEmpty() && ans.length() >= 5; // Mark correct if not empty and >= 5 characters
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
}
