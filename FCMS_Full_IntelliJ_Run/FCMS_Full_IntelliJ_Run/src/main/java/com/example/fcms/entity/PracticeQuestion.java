package com.example.fcms.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "practice_questions")
public class PracticeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_question_id")
    private Long practiceQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_session_id", nullable = false)
    private PracticeSession practiceSession;

    @Column(name = "question_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    // MCQ / TRUE_FALSE / SHORT_ANSWER
    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    @Column(name = "correct_answer", columnDefinition = "NVARCHAR(MAX)")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "NVARCHAR(MAX)")
    private String explanation;

    @Column(name = "student_answer", columnDefinition = "NVARCHAR(MAX)")
    private String studentAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}