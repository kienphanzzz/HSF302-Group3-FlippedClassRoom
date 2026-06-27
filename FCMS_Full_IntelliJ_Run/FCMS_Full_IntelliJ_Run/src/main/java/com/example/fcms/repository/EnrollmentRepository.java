package com.example.fcms.repository;

import com.example.fcms.entity.ClassRoom;
import com.example.fcms.entity.Enrollment;
import com.example.fcms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentAndStatus(User student, String status);

    boolean existsByStudentAndClassRoomAndStatus(User student, ClassRoom classRoom, String status);

    long countByClassRoomAndStatus(ClassRoom classRoom, String status);
}
