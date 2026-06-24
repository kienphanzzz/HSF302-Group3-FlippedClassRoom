package com.example.fcms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "practice_sessions")
public class PracticeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_session_id")
    private Long practiceSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private ContentResource content;

    // EASY / MEDIUM / HARD
    @Builder.Default
    @Column(name = "difficulty", nullable = false, length = 20)
    private String difficulty = "MEDIUM";

    // MCQ / TRUE_FALSE / SHORT_ANSWER
    @Builder.Default
    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType = "MCQ";

    @Builder.Default
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 0;

    @Builder.Default
    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}