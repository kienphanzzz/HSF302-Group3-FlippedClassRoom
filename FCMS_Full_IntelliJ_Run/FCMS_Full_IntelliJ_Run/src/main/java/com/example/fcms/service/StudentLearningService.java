package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentLearningService {

    private final EnrollmentRepository enrollmentRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;
    private final UserRepository userRepository;

    public StudentLearningService(EnrollmentRepository enrollmentRepository,
                                  LearningNodeRepository learningNodeRepository,
                                  ContentResourceRepository contentResourceRepository,
                                  LearningProgressLogRepository learningProgressLogRepository,
                                  UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.userRepository = userRepository;
    }

    public List<ClassRoom> getJoinedClasses(Long studentId) {
        return enrollmentRepository.findByStudent_UserIdAndStatus(studentId, "ACTIVE")
                .stream()
                .map(Enrollment::getClassRoom)
                .collect(Collectors.toList());
    }

    public boolean isStudentEnrolled(Long studentId, Long classId) {
        return enrollmentRepository.findByClassRoom_ClassIdAndStudent_UserId(classId, studentId)
                .map(e -> "ACTIVE".equals(e.getStatus()))
                .orElse(false);
    }

    public List<LearningNode> getLearningNodes(Long studentId, Long classId) {
        if (!isStudentEnrolled(studentId, classId)) {
            throw new IllegalArgumentException("Student is not enrolled in this class.");
        }
        return learningNodeRepository.findByClassRoom_ClassIdAndVisibleTrueOrderByOrderIndexAsc(classId);
    }

    public Optional<LearningNode> getLearningNode(Long studentId, Long nodeId) {
        Optional<LearningNode> nodeOpt = learningNodeRepository.findById(nodeId);
        if (nodeOpt.isPresent()) {
            LearningNode node = nodeOpt.get();
            if (isStudentEnrolled(studentId, node.getClassRoom().getClassId())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public List<ContentResource> getContentResources(Long studentId, Long nodeId) {
        Optional<LearningNode> nodeOpt = getLearningNode(studentId, nodeId);
        if (nodeOpt.isEmpty()) {
            throw new IllegalArgumentException("Student does not have access to this learning node.");
        }
        return contentResourceRepository.findByLearningNode_NodeIdAndVisibleTrue(nodeId);
    }

    public Optional<ContentResource> getContentResource(Long studentId, Long contentId) {
        Optional<ContentResource> resourceOpt = contentResourceRepository.findById(contentId);
        if (resourceOpt.isPresent()) {
            ContentResource resource = resourceOpt.get();
            if (isStudentEnrolled(studentId, resource.getLearningNode().getClassRoom().getClassId())) {
                return Optional.of(resource);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void logProgress(Long studentId, Long contentId) {
        Optional<ContentResource> contentOpt = getContentResource(studentId, contentId);
        if (contentOpt.isEmpty()) {
            throw new IllegalArgumentException("Student does not have access to this content resource.");
        }

        ContentResource resource = contentOpt.get();
        String activityType = "VIEW_CONTENT";
        if ("VIDEO_URL".equalsIgnoreCase(resource.getContentType())) {
            activityType = "WATCH_VIDEO";
        } else if ("EXTERNAL_LINK".equalsIgnoreCase(resource.getContentType()) || 
                   "DOCUMENT_URL".equalsIgnoreCase(resource.getContentType()) || 
                   "MEETING_URL".equalsIgnoreCase(resource.getContentType())) {
            activityType = "OPEN_LINK";
        }

        boolean alreadyLogged = learningProgressLogRepository.existsByStudent_UserIdAndContent_ContentIdAndActivityType(
                studentId, contentId, activityType);

        if (!alreadyLogged) {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            LearningProgressLog log = LearningProgressLog.builder()
                    .student(student)
                    .classRoom(resource.getLearningNode().getClassRoom())
                    .learningNode(resource.getLearningNode())
                    .content(resource)
                    .activityType(activityType)
                    .progressPercent(BigDecimal.valueOf(100.00))
                    .build();

            learningProgressLogRepository.save(log);
        }
    }

    public long getViewedResourcesCountForClass(Long studentId, Long classId) {
        return learningProgressLogRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId)
                .stream()
                .filter(log -> log.getClassRoom().getClassId().equals(classId))
                .filter(log -> log.getContent() != null)
                .filter(log -> "VIEW_CONTENT".equalsIgnoreCase(log.getActivityType()) ||
                               "WATCH_VIDEO".equalsIgnoreCase(log.getActivityType()) ||
                               "OPEN_LINK".equalsIgnoreCase(log.getActivityType()))
                .map(log -> log.getContent().getContentId())
                .distinct()
                .count();
    }

    public List<ContentResource> getAvailableContentResources(Long studentId) {
        List<ClassRoom> classes = getJoinedClasses(studentId);
        return classes.stream()
                .flatMap(c -> learningNodeRepository.findByClassRoom_ClassIdAndVisibleTrueOrderByOrderIndexAsc(c.getClassId()).stream())
                .flatMap(n -> contentResourceRepository.findByLearningNode_NodeIdAndVisibleTrue(n.getNodeId()).stream())
                .collect(Collectors.toList());
    }

    public List<LearningNode> getAvailableLearningNodes(Long studentId) {
        List<ClassRoom> classes = getJoinedClasses(studentId);
        return classes.stream()
                .flatMap(c -> learningNodeRepository.findByClassRoom_ClassIdAndVisibleTrueOrderByOrderIndexAsc(c.getClassId()).stream())
                .collect(Collectors.toList());
    }
}
