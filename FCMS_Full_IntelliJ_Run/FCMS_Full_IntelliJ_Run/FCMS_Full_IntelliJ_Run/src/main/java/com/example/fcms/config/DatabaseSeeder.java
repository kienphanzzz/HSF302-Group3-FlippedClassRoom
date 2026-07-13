package com.example.fcms.config;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final AssignmentRepository assignmentRepository;

    public DatabaseSeeder(UserRepository userRepository,
                          ClassRoomRepository classRoomRepository,
                          EnrollmentRepository enrollmentRepository,
                          LearningNodeRepository learningNodeRepository,
                          ContentResourceRepository contentResourceRepository,
                          AssignmentRepository assignmentRepository) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Check if demo student already exists to avoid duplication
        if (userRepository.findByEmail("demo-student@fcms.local").isPresent()) {
            return;
        }

        // 1. Create Teacher
        User teacher = User.builder()
                .fullName("Prof. Tran Phuong")
                .email("teacher@fcms.local")
                .passwordHash("123") // simple password hash for demo
                .role("TEACHER")
                .status("ACTIVE")
                .build();
        teacher = userRepository.save(teacher);

        // 2. Create Student (Should auto-increment to ID = 2 if DB is empty)
        User student = User.builder()
                .fullName("Alex Nguyen")
                .email("demo-student@fcms.local")
                .passwordHash("123")
                .role("STUDENT")
                .status("ACTIVE")
                .build();
        student = userRepository.save(student);

        // 3. Create Classes
        ClassRoom class1 = ClassRoom.builder()
                .className("Software Engineering")
                .subjectCode("SWE301")
                .classCode("SE2024")
                .teacher(teacher)
                .status("ACTIVE")
                .description("Pre-class flip classroom for Software Engineering concepts.")
                .build();
        class1 = classRoomRepository.save(class1);

        ClassRoom class2 = ClassRoom.builder()
                .className("Data Structures & Algorithms")
                .subjectCode("DSA301")
                .classCode("DSA301")
                .teacher(teacher)
                .status("ACTIVE")
                .description("Pre-class flip classroom for Algorithms concepts.")
                .build();
        class2 = classRoomRepository.save(class2);

        // 4. Enroll Student in Classes
        Enrollment enroll1 = Enrollment.builder()
                .classRoom(class1)
                .student(student)
                .status("ACTIVE")
                .build();
        enrollmentRepository.save(enroll1);

        Enrollment enroll2 = Enrollment.builder()
                .classRoom(class2)
                .student(student)
                .status("ACTIVE")
                .build();
        enrollmentRepository.save(enroll2);

        // 5. Create Learning Nodes for Class 1 (Software Engineering)
        LearningNode node1 = LearningNode.builder()
                .classRoom(class1)
                .title("Topic 1: Introduction to Software Engineering")
                .description("Overview of software engineering lifecycle, agile methodologies, and course syllabus.")
                .orderIndex(0)
                .suggestedStartDate(LocalDate.now().minusDays(5))
                .suggestedEndDate(LocalDate.now().minusDays(1))
                .visible(true)
                .build();
        node1 = learningNodeRepository.save(node1);

        LearningNode node2 = LearningNode.builder()
                .classRoom(class1)
                .title("Topic 2: Requirements Engineering")
                .description("Elicitation, analysis, specification (SRS), and validation of requirements. Focus on Use Case diagrams.")
                .orderIndex(1)
                .suggestedStartDate(LocalDate.now())
                .suggestedEndDate(LocalDate.now().plusDays(5))
                .visible(true)
                .build();
        node2 = learningNodeRepository.save(node2);

        LearningNode node3 = LearningNode.builder()
                .classRoom(class1)
                .title("Topic 3: Analysis Models")
                .description("Structural and behavioral modeling using UML Class diagrams and Activity diagrams.")
                .orderIndex(2)
                .suggestedStartDate(LocalDate.now().plusDays(6))
                .suggestedEndDate(LocalDate.now().plusDays(10))
                .visible(true)
                .build();
        node3 = learningNodeRepository.save(node3);

        // 6. Create Content Resources for Nodes
        ContentResource res1 = ContentResource.builder()
                .learningNode(node1)
                .uploadedBy(teacher)
                .title("What is Software Engineering?")
                .description("Introductory video outlining the software engineering profession.")
                .contentType("VIDEO_URL")
                .externalUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .durationMinutes(12)
                .visible(true)
                .build();
        contentResourceRepository.save(res1);

        ContentResource res2 = ContentResource.builder()
                .learningNode(node1)
                .uploadedBy(teacher)
                .title("Course Overview & Syllabus")
                .description("Syllabus document specifying grading criteria, project guidelines, and roadmap.")
                .contentType("DOCUMENT_URL")
                .externalUrl("https://example.com/syllabus.pdf")
                .durationMinutes(10)
                .visible(true)
                .build();
        contentResourceRepository.save(res2);

        ContentResource res3 = ContentResource.builder()
                .learningNode(node2)
                .uploadedBy(teacher)
                .title("Use Case Diagrams — Lecture Slides")
                .description("Comprehensive guide to drawing UML Use Case diagrams, actors, associations, and use cases.")
                .contentType("DOCUMENT_URL")
                .externalUrl("https://example.com/use-case.pdf")
                .durationMinutes(15)
                .visible(true)
                .build();
        contentResourceRepository.save(res3);

        ContentResource res4 = ContentResource.builder()
                .learningNode(node2)
                .uploadedBy(teacher)
                .title("Meet URL")
                .description("Google Meet link for online Q&A session.")
                .contentType("MEETING_URL")
                .externalUrl("https://meet.google.com/abc-defg-hij")
                .durationMinutes(60)
                .visible(true)
                .build();
        contentResourceRepository.save(res4);

        // 7. Create Assignments
        Assignment assign1 = Assignment.builder()
                .learningNode(node2)
                .teacher(teacher)
                .title("Write a Software Requirements Specification Document")
                .description("Create a complete SRS document for a selected software system. Your document should include project scope, actors, functional requirements, non-functional requirements, use cases, and business rules.")
                .deadline(LocalDateTime.now().plusDays(2)) // 2 days from now
                .maxScore(BigDecimal.valueOf(10.00))
                .allowLateSubmission(true)
                .status("ACTIVE")
                .build();
        assignmentRepository.save(assign1);

        Assignment assign2 = Assignment.builder()
                .learningNode(node3)
                .teacher(teacher)
                .title("Activity Diagram Assignment")
                .description("Create an activity diagram based on the business logic description.")
                .deadline(LocalDateTime.now().plusDays(7)) // 7 days from now
                .maxScore(BigDecimal.valueOf(10.00))
                .allowLateSubmission(true)
                .status("ACTIVE")
                .build();
        assignmentRepository.save(assign2);
    }
}
