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

    @Column(name = "option_a", length = 1000)
    private String optionA;

    @Column(name = "option_b", length = 1000)
    private String optionB;

    @Column(name = "option_c", length = 1000)
    private String optionC;

    @Column(name = "option_d", length = 1000)
    private String optionD;

    @Column(name = "correct_answer", columnDefinition = "NVARCHAR(MAX)")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "NVARCHAR(MAX)")
    private String explanation;

    @Column(name = "student_answer", columnDefinition = "NVARCHAR(MAX)")
    private String studentAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "answered_at")
    private java.time.LocalDateTime answeredAt;

    @Column(name = "revealed")
    private Boolean revealed;

    @Transient
    @Builder.Default
    private java.util.List<PracticeQuestionChat> chats = new java.util.ArrayList<>();

    @Transient
    public String getStatusClass() {
        if (Boolean.TRUE.equals(isCorrect)) {
            return "question-correct";
        } else if (Boolean.FALSE.equals(isCorrect)) {
            return "question-wrong";
        } else if (Boolean.TRUE.equals(revealed)) {
            return "question-revealed";
        }
        return "";
    }
}