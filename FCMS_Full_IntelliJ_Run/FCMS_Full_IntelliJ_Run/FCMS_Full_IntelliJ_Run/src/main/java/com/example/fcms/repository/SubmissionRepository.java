package com.example.fcms.repository;

import com.example.fcms.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByAssignment_AssignmentIdAndStudent_UserId(Long assignmentId, Long studentId);
    List<Submission> findByStudent_UserId(Long studentId);
}
