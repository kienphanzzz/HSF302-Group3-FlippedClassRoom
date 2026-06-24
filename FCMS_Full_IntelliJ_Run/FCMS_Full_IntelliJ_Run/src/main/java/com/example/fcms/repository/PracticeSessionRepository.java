package com.example.fcms.repository;

import com.example.fcms.entity.PracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {
    List<PracticeSession> findByStudent_UserIdOrderByCreatedAtDesc(Long studentId);
    Optional<PracticeSession> findByPracticeSessionIdAndStudent_UserId(Long sessionId, Long studentId);
}
