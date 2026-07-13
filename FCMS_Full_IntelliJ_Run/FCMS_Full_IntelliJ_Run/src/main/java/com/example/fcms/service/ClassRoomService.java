package com.example.fcms.service;

import com.example.fcms.dto.classroom.ClassRoomResponse;
import com.example.fcms.dto.classroom.CreateClassRequest;
import com.example.fcms.dto.classroom.JoinClassRequest;
import com.example.fcms.entity.ClassRoom;
import com.example.fcms.entity.Enrollment;
import com.example.fcms.entity.User;
import com.example.fcms.repository.ClassRoomRepository;
import com.example.fcms.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class ClassRoomService {

    private static final String ACTIVE = "ACTIVE";
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final ClassRoomRepository classRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SecureRandom random = new SecureRandom();

    public ClassRoomService(ClassRoomRepository classRoomRepository, EnrollmentRepository enrollmentRepository) {
        this.classRoomRepository = classRoomRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public ClassRoomResponse createClass(CreateClassRequest request, User teacher) {
        if (!"TEACHER".equals(teacher.getRole())) {
            throw new SecurityException("Only teachers can create classes");
        }

        ClassRoom classRoom = ClassRoom.builder()
                .teacher(teacher)
                .className(request.getClassName().trim())
                .subjectCode(cleanText(request.getSubjectCode()))
                .description(cleanText(request.getDescription()))
                .classCode(generateUniqueClassCode())
                .status(ACTIVE)
                .build();

        ClassRoom savedClass = classRoomRepository.save(classRoom);
        return toResponse(savedClass);
    }

    public ClassRoomResponse joinClass(JoinClassRequest request, User student) {
        if (!"STUDENT".equals(student.getRole())) {
            throw new SecurityException("Only students can join classes");
        }

        String classCode = request.getClassCode().trim().toUpperCase();
        ClassRoom classRoom = classRoomRepository.findByClassCodeAndStatus(classCode, ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class code"));

        boolean alreadyJoined = enrollmentRepository.existsByStudentAndClassRoomAndStatus(student, classRoom, ACTIVE);
        if (alreadyJoined) {
            throw new IllegalArgumentException("You already joined this class");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .classRoom(classRoom)
                .status(ACTIVE)
                .build();

        enrollmentRepository.save(enrollment);
        return toResponse(classRoom);
    }

    public List<ClassRoomResponse> getTeacherClasses(User teacher) {
        return classRoomRepository.findByTeacherAndStatus(teacher, ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClassRoomResponse> getStudentClasses(User student) {
        return enrollmentRepository.findByStudentAndStatus(student, ACTIVE)
                .stream()
                .map(Enrollment::getClassRoom)
                .map(this::toResponse)
                .toList();
    }

    public long countTeacherStudents(User teacher) {
        return classRoomRepository.findByTeacherAndStatus(teacher, ACTIVE)
                .stream()
                .mapToLong(classRoom -> enrollmentRepository.countByClassRoomAndStatus(classRoom, ACTIVE))
                .sum();
    }

    private ClassRoomResponse toResponse(ClassRoom classRoom) {
        ClassRoomResponse response = new ClassRoomResponse();
        response.setClassId(classRoom.getClassId());
        response.setClassName(classRoom.getClassName());
        response.setSubjectCode(classRoom.getSubjectCode());
        response.setDescription(classRoom.getDescription());
        response.setClassCode(classRoom.getClassCode());
        response.setTeacherName(classRoom.getTeacher().getFullName());
        response.setStudentCount(enrollmentRepository.countByClassRoomAndStatus(classRoom, ACTIVE));
        return response;
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String generateUniqueClassCode() {
        String code;
        do {
            code = generateClassCode();
        } while (classRoomRepository.existsByClassCode(code));

        return code;
    }

    private String generateClassCode() {
        StringBuilder code = new StringBuilder();

        for (int index = 0; index < 6; index++) {
            int randomIndex = random.nextInt(CODE_CHARS.length());
            code.append(CODE_CHARS.charAt(randomIndex));
        }

        return code.toString();
    }
}
