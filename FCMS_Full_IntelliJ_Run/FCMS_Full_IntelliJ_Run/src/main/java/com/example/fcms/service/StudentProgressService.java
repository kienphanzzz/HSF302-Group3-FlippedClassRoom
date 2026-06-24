package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;

    public StudentProgressService(EnrollmentRepository enrollmentRepository,
                                  SubmissionRepository submissionRepository,
                                  PracticeSessionRepository practiceSessionRepository,
                                  LearningProgressLogRepository learningProgressLogRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.practiceSessionRepository = practiceSessionRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
    }

    public Map<String, Object> getProgressSummary(Long studentId) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Joined Classes
        long joinedClassesCount = enrollmentRepository.countByStudent_UserIdAndStatus(studentId, "ACTIVE");
        stats.put("joinedClassesCount", joinedClassesCount);

        // 2. Fetch all logs for calculations
        List<LearningProgressLog> logs = learningProgressLogRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId);

        // Unique content items viewed / watched / opened
        long viewedContentCount = logs.stream()
                .filter(log -> log.getContent() != null)
                .filter(log -> "VIEW_CONTENT".equalsIgnoreCase(log.getActivityType()) ||
                               "WATCH_VIDEO".equalsIgnoreCase(log.getActivityType()) ||
                               "OPEN_LINK".equalsIgnoreCase(log.getActivityType()))
                .map(log -> log.getContent().getContentId())
                .distinct()
                .count();
        stats.put("viewedContentCount", viewedContentCount);

        // 3. Submissions
        List<Submission> submissions = submissionRepository.findByStudent_UserId(studentId);
        long submittedAssignmentsCount = submissions.size();
        stats.put("submittedAssignmentsCount", submittedAssignmentsCount);

        // Late submissions
        long lateSubmissionsCount = submissions.stream()
                .filter(sub -> "LATE".equalsIgnoreCase(sub.getSubmissionStatus()))
                .count();
        stats.put("lateSubmissionsCount", lateSubmissionsCount);

        // 4. AI Practice Sessions
        long aiPracticeSessionsCount = practiceSessionRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId).size();
        stats.put("aiPracticeSessionsCount", aiPracticeSessionsCount);

        // 5. Recent activities (Limit to 10)
        List<LearningProgressLog> recentActivities = logs.stream()
                .limit(10)
                .collect(Collectors.toList());
        stats.put("recentActivities", recentActivities);

        return stats;
    }
}
