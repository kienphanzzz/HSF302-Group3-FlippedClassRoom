package com.example.fcms.repository;

import com.example.fcms.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTeacher_UserIdOrderByCreatedAtDesc(Long teacherId);
    List<Question> findByTeacher_UserIdAndDifficultyOrderByCreatedAtDesc(Long teacherId, String difficulty);
    List<Question> findByTeacher_UserIdAndQuestionTypeOrderByCreatedAtDesc(Long teacherId, String questionType);
}
