package com.example.fcms.repository;

import com.example.fcms.entity.ClassRoom;
import com.example.fcms.entity.Enrollment;
import com.example.fcms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent_UserIdAndStatus(Long studentId, String status);
    long countByStudent_UserIdAndStatus(Long studentId, String status);
    Optional<Enrollment> findByClassRoom_ClassIdAndStudent_UserId(Long classId, Long studentId);

    List<Enrollment> findByStudentAndStatus(User student, String status);
    boolean existsByStudentAndClassRoomAndStatus(User student, ClassRoom classRoom, String status);
    long countByClassRoomAndStatus(ClassRoom classRoom, String status);
}

