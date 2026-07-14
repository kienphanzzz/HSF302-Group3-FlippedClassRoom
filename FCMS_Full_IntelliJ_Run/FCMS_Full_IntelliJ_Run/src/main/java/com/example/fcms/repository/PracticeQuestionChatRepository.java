package com.example.fcms.repository;

import com.example.fcms.entity.PracticeQuestionChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeQuestionChatRepository extends JpaRepository<PracticeQuestionChat, Long> {
    List<PracticeQuestionChat> findByPracticeQuestion_PracticeQuestionIdOrderByCreatedAtAsc(Long questionId);
    List<PracticeQuestionChat> findByPracticeSession_PracticeSessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteByPracticeSession_PracticeSessionId(Long sessionId);
    void deleteByPracticeQuestion_PracticeQuestionId(Long questionId);
}
