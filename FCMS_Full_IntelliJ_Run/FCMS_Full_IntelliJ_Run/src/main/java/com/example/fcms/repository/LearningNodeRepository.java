package com.example.fcms.repository;

import com.example.fcms.entity.LearningNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningNodeRepository extends JpaRepository<LearningNode, Long> {
    List<LearningNode> findByClassRoom_ClassIdAndVisibleTrueOrderByOrderIndexAsc(Long classId);
    List<LearningNode> findByClassRoomClassIdOrderByOrderIndexAsc(Long classId);
}
