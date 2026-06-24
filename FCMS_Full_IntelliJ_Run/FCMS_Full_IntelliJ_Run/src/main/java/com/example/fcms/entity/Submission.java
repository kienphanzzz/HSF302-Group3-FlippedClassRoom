package com.example.fcms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "submissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_assignment_student", columnNames = {"assignment_id", "student_id"})
        }
)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "answer_text", columnDefinition = "NVARCHAR(MAX)")
    private String answerText;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    // ON_TIME / LATE / MISSING
    @Builder.Default
    @Column(name = "submission_status", nullable = false, length = 20)
    private String submissionStatus = "ON_TIME";

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "feedback", columnDefinition = "NVARCHAR(MAX)")
    private String feedback;

    @Column(name = "ai_feedback_suggestion", columnDefinition = "NVARCHAR(MAX)")
    private String aiFeedbackSuggestion;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;
}