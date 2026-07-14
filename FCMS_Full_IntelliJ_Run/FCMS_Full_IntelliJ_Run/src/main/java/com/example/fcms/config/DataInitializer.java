package com.example.fcms.config;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ContentResourceRepository contentResourceRepository;

    public DataInitializer(UserRepository userRepository,
                           ClassRoomRepository classRoomRepository,
                           LearningNodeRepository learningNodeRepository,
                           ContentResourceRepository contentResourceRepository) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.contentResourceRepository = contentResourceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create uploads folder if not exists
        File uploadDir = new File("uploads");
        System.out.println("=================================================");
        System.out.println("FCMS Uploads Absolute Path: " + uploadDir.getAbsolutePath());
        System.out.println("=================================================");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Initialize Users if not present
        if (userRepository.count() == 0) {
            User teacher = User.builder()
                    .fullName("Prof. Tran Phuong")
                    .email("teacher@fe.edu.vn")
                    .passwordHash("123456") // Placed plain text for ease of login/testing demo before Person 1 implements BCrypt
                    .role("TEACHER")
                    .status("ACTIVE")
                    .build();
            userRepository.save(teacher);

            User student = User.builder()
                    .fullName("Alex Nguyen")
                    .email("student@fpt.edu.vn")
                    .passwordHash("123456")
                    .role("STUDENT")
                    .status("ACTIVE")
                    .build();
            userRepository.save(student);

            // Initialize ClassRoom
            ClassRoom classRoom = ClassRoom.builder()
                    .teacher(teacher)
                    .className("Software Engineering")
                    .subjectCode("SE2024")
                    .description("Learn fundamental concepts of software engineering, including requirements, design patterns, and agile methodologies.")
                    .classCode("SE2024")
                    .status("ACTIVE")
                    .build();
            classRoomRepository.save(classRoom);

            // Initialize Learning Nodes (Topics)
            LearningNode topic1 = LearningNode.builder()
                    .classRoom(classRoom)
                    .title("Topic 1: Introduction to Software Engineering")
                    .description("Overview of the software process models, SDLC phases, and team organization.")
                    .orderIndex(1)
                    .suggestedStartDate(LocalDate.now().minusDays(10))
                    .suggestedEndDate(LocalDate.now().minusDays(5))
                    .visible(true)
                    .build();
            learningNodeRepository.save(topic1);

            LearningNode topic2 = LearningNode.builder()
                    .classRoom(classRoom)
                    .title("Topic 2: Requirements Engineering")
                    .description("Eliciting, documenting, and validating software requirements.")
                    .orderIndex(2)
                    .suggestedStartDate(LocalDate.now().minusDays(4))
                    .suggestedEndDate(LocalDate.now().plusDays(3))
                    .visible(true)
                    .build();
            learningNodeRepository.save(topic2);

            // Initialize Content Resources
            ContentResource resource1 = ContentResource.builder()
                    .learningNode(topic1)
                    .uploadedBy(teacher)
                    .title("What is Software Engineering?")
                    .description("Brief video covering basic concepts.")
                    .contentType("VIDEO_URL")
                    .externalUrl("https://www.youtube.com/embed/dQw4w9WgXcQ") // example embed youtube URL
                    .visible(true)
                    .build();
            contentResourceRepository.save(resource1);

            ContentResource resource2 = ContentResource.builder()
                    .learningNode(topic1)
                    .uploadedBy(teacher)
                    .title("Syllabus & Lecture Slides")
                    .description("Official slides for Topic 1.")
                    .contentType("DOCUMENT_URL")
                    .externalUrl("https://docs.google.com/presentation/d/123456")
                    .visible(true)
                    .build();
            contentResourceRepository.save(resource2);
        }
    }
}
