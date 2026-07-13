package com.example.fcms.repository;

import com.example.fcms.entity.PracticeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {
    List<PracticeQuestion> findByPracticeSession_PracticeSessionId(Long sessionId);
    void deleteByPracticeSession_PracticeSessionId(Long sessionId);
}
