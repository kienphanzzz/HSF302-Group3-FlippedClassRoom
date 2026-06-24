package com.example.fcms.repository;

import com.example.fcms.entity.LearningProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface LearningProgressLogRepository extends JpaRepository<LearningProgressLog, Long> {
    List<LearningProgressLog> findByStudentUserIdAndClassRoomClassId(Long studentId, Long classId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM learning_progress_logs WHERE content_id = :contentId", nativeQuery = true)
    void deleteLogsByContentId(@Param("contentId") Long contentId);
}
