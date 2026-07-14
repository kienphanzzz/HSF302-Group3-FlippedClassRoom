package com.example.fcms.repository;

import com.example.fcms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByLearningNode_ClassRoom_ClassIdIn(List<Long> classIds);
    List<Assignment> findByLearningNode_NodeIdAndStatus(Long nodeId, String status);
}
