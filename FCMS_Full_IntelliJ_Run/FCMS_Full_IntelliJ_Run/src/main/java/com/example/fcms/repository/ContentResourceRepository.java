package com.example.fcms.repository;

import com.example.fcms.entity.ContentResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ContentResourceRepository extends JpaRepository<ContentResource, Long> {
    List<ContentResource> findByLearningNode_NodeIdAndVisibleTrue(Long nodeId);
    List<ContentResource> findByLearningNodeNodeId(Long nodeId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM practice_questions WHERE practice_session_id IN (SELECT practice_session_id FROM practice_sessions WHERE content_id = :contentId)", nativeQuery = true)
    void deletePracticeQuestionsByContentId(@Param("contentId") Long contentId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM practice_sessions WHERE content_id = :contentId", nativeQuery = true)
    void deletePracticeSessionsByContentId(@Param("contentId") Long contentId);
}
