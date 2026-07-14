package com.example.fcms.repository;

import com.example.fcms.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestion_QuestionId(Long questionId);
    void deleteByQuestion_QuestionId(Long questionId);
}
