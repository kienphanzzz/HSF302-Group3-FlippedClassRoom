package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;
    private final UserRepository userRepository;

    public StudentAssignmentService(AssignmentRepository assignmentRepository,
                                    SubmissionRepository submissionRepository,
                                    EnrollmentRepository enrollmentRepository,
                                    LearningProgressLogRepository learningProgressLogRepository,
                                    UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.userRepository = userRepository;
    }

    private boolean isStudentEnrolledInAssignmentClass(Long studentId, Assignment assignment) {
        Long classId = assignment.getLearningNode().getClassRoom().getClassId();
        return enrollmentRepository.findByClassRoom_ClassIdAndStudent_UserId(classId, studentId)
                .map(e -> "ACTIVE".equals(e.getStatus()))
                .orElse(false);
    }

    public List<Assignment> getAssignmentsForStudent(Long studentId) {
        List<Long> classIds = enrollmentRepository.findByStudent_UserIdAndStatus(studentId, "ACTIVE")
                .stream()
                .map(e -> e.getClassRoom().getClassId())
                .collect(Collectors.toList());

        if (classIds.isEmpty()) {
            return List.of();
        }

        return assignmentRepository.findByLearningNode_ClassRoom_ClassIdIn(classIds);
    }

    public Optional<Assignment> getAssignment(Long studentId, Long assignmentId) {
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isPresent()) {
            Assignment assignment = assignmentOpt.get();
            if (isStudentEnrolledInAssignmentClass(studentId, assignment)) {
                return Optional.of(assignment);
            }
        }
        return Optional.empty();
    }

    public Optional<Submission> getSubmissionForAssignment(Long studentId, Long assignmentId) {
        return submissionRepository.findByAssignment_AssignmentIdAndStudent_UserId(assignmentId, studentId);
    }

    public List<Submission> getSubmissionsForStudent(Long studentId) {
        return submissionRepository.findByStudent_UserId(studentId);
    }

    public Optional<Submission> getSubmission(Long studentId, Long submissionId) {
        Optional<Submission> submissionOpt = submissionRepository.findById(submissionId);
        if (submissionOpt.isPresent()) {
            Submission submission = submissionOpt.get();
            if (submission.getStudent().getUserId().equals(studentId)) {
                return Optional.of(submission);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Submission submitAssignment(Long studentId, Long assignmentId, String answerText, String filePath, String originalFileName) {
        Assignment assignment = getAssignment(studentId, assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found or student is not enrolled in the class."));

        LocalDateTime now = LocalDateTime.now();
        Optional<Submission> existingSubmissionOpt = submissionRepository.findByAssignment_AssignmentIdAndStudent_UserId(assignmentId, studentId);

        if (existingSubmissionOpt.isPresent()) {
            Submission existing = existingSubmissionOpt.get();
            // Edit check: Only allow update before deadline
            if (now.isAfter(assignment.getDeadline())) {
                throw new IllegalStateException("The deadline has passed. You cannot update your submission.");
            }
            existing.setAnswerText(answerText);
            existing.setFilePath(filePath);
            existing.setOriginalFileName(originalFileName);
            existing.setSubmittedAt(now);
            existing.setSubmissionStatus("ON_TIME"); // If it's before deadline, it's ON_TIME
            return submissionRepository.save(existing);
        } else {
            // New submission check: If past deadline, check if late submission is allowed
            String submissionStatus = "ON_TIME";
            if (now.isAfter(assignment.getDeadline())) {
                if (!Boolean.TRUE.equals(assignment.getAllowLateSubmission())) {
                    throw new IllegalStateException("Late submissions are not allowed for this assignment.");
                }
                submissionStatus = "LATE";
            }

            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

            Submission submission = Submission.builder()
                    .assignment(assignment)
                    .student(student)
                    .answerText(answerText)
                    .filePath(filePath)
                    .originalFileName(originalFileName)
                    .submittedAt(now)
                    .submissionStatus(submissionStatus)
                    .build();

            Submission saved = submissionRepository.save(submission);

            // Log activity progress
            LearningProgressLog log = LearningProgressLog.builder()
                    .student(student)
                    .classRoom(assignment.getLearningNode().getClassRoom())
                    .learningNode(assignment.getLearningNode())
                    .activityType("SUBMIT_ASSIGNMENT")
                    .progressPercent(BigDecimal.valueOf(100.00))
                    .build();
            learningProgressLogRepository.save(log);

            return saved;
        }
    }

    public List<Assignment> getAssignmentsForNode(Long nodeId) {
        return assignmentRepository.findByLearningNode_NodeIdAndStatus(nodeId, "ACTIVE");
    }
}
