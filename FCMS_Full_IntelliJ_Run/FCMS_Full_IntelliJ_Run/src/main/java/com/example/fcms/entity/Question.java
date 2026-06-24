package com.example.fcms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private LearningNode learningNode;

    @Column(name = "question_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    // MCQ / TRUE_FALSE / SHORT_ANSWER / ESSAY
    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    // EASY / MEDIUM / HARD
    @Builder.Default
    @Column(name = "difficulty", nullable = false, length = 20)
    private String difficulty = "MEDIUM";

    @Column(name = "correct_answer", columnDefinition = "NVARCHAR(MAX)")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "NVARCHAR(MAX)")
    private String explanation;

    // MANUAL / AI_GENERATED
    @Builder.Default
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType = "MANUAL";

    // DRAFT / APPROVED / ARCHIVED
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}