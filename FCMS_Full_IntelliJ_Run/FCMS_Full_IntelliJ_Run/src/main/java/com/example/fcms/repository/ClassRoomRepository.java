package com.example.fcms.repository;

import com.example.fcms.entity.ClassRoom;
import com.example.fcms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    Optional<ClassRoom> findByClassCode(String classCode);

    List<ClassRoom> findByTeacherAndStatus(User teacher, String status);

    Optional<ClassRoom> findByClassCodeAndStatus(String classCode, String status);

    boolean existsByClassCode(String classCode);
}
