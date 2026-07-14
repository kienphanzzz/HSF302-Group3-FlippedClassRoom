package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TeacherProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    public TeacherProgressService(EnrollmentRepository enrollmentRepository,
                                  LearningNodeRepository learningNodeRepository,
                                  ContentResourceRepository contentResourceRepository,
                                  LearningProgressLogRepository learningProgressLogRepository,
                                  PracticeSessionRepository practiceSessionRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.practiceSessionRepository = practiceSessionRepository;
    }

    public List<StudentProgressDTO> getStudentsProgress(Long classId) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassRoom_ClassIdAndStatus(classId, "ACTIVE");
        
        // 1. Get all visible content resources in this classroom
        List<LearningNode> nodes = learningNodeRepository.findByClassRoomClassIdOrderByOrderIndexAsc(classId);
        List<ContentResource> visibleContents = new ArrayList<>();
        for (LearningNode node : nodes) {
            if (node.getVisible() != null && node.getVisible()) {
                visibleContents.addAll(contentResourceRepository.findByLearningNode_NodeIdAndVisibleTrue(node.getNodeId()));
            }
        }
        
        List<Long> visibleContentIds = visibleContents.stream()
                .map(ContentResource::getContentId)
                .collect(Collectors.toList());

        List<StudentProgressDTO> progressList = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            Long studentId = student.getUserId();

            // Calculate prep completion
            List<LearningProgressLog> logs = learningProgressLogRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId);
            Set<Long> viewedContentIds = logs.stream()
                    .filter(log -> log.getContent() != null)
                    .filter(log -> "VIEW_CONTENT".equalsIgnoreCase(log.getActivityType()) ||
                                   "WATCH_VIDEO".equalsIgnoreCase(log.getActivityType()) ||
                                   "OPEN_LINK".equalsIgnoreCase(log.getActivityType()))
                    .map(log -> log.getContent().getContentId())
                    .collect(Collectors.toSet());

            long viewedCountInClass = visibleContentIds.stream()
                    .filter(viewedContentIds::contains)
                    .count();

            double prepRate = visibleContentIds.isEmpty() ? 100.0 : (100.0 * viewedCountInClass / visibleContentIds.size());

            // AI Practice Session calculations
            List<PracticeSession> sessions = practiceSessionRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId);
            int aiCount = sessions.size();

            double totalAccuracy = 0;
            int completedCount = 0;
            for (PracticeSession s : sessions) {
                if ("COMPLETED".equals(s.getStatus()) && s.getTotalQuestions() != null && s.getTotalQuestions() > 0) {
                    double sessionAccuracy = (s.getCorrectAnswers() * 100.0) / s.getTotalQuestions();
                    totalAccuracy += sessionAccuracy;
                    completedCount++;
                }
            }
            double avgAccuracy = (completedCount == 0) ? 0.0 : (totalAccuracy / completedCount);
            
            boolean atRisk = prepRate < 40.0;

            progressList.add(new StudentProgressDTO(
                    student.getFullName(),
                    student.getEmail(),
                    Math.round(prepRate * 10.0) / 10.0,
                    aiCount,
                    Math.round(avgAccuracy * 10.0) / 10.0,
                    atRisk
            ));
        }

        return progressList;
    }

    @lombok.Value
    public static class StudentProgressDTO {
        String studentName;
        String studentEmail;
        double prepRate;
        int aiCount;
        double avgAccuracy;
        boolean atRisk;
    }
}
