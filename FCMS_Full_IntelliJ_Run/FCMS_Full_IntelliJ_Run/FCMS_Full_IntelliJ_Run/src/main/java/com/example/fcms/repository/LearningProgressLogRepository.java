package com.example.fcms.repository;

import com.example.fcms.entity.LearningProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningProgressLogRepository extends JpaRepository<LearningProgressLog, Long> {
    List<LearningProgressLog> findByStudent_UserIdOrderByCreatedAtDesc(Long studentId);
    long countByStudent_UserIdAndActivityType(Long studentId, String activityType);
    boolean existsByStudent_UserIdAndContent_ContentIdAndActivityType(Long studentId, Long contentId, String activityType);
}
