package com.example.fcms.service;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PracticeService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;
    private final UserRepository userRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final FileTextExtractionService fileTextExtractionService;

    public PracticeService(PracticeSessionRepository practiceSessionRepository,
                           PracticeQuestionRepository practiceQuestionRepository,
                           ContentResourceRepository contentResourceRepository,
                           EnrollmentRepository enrollmentRepository,
                           LearningProgressLogRepository learningProgressLogRepository,
                           UserRepository userRepository,
                           LearningNodeRepository learningNodeRepository,
                           FileTextExtractionService fileTextExtractionService) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.userRepository = userRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.fileTextExtractionService = fileTextExtractionService;
    }

    private boolean isStudentEnrolledInContentClass(Long studentId, ContentResource resource) {
        Long classId = resource.getLearningNode().getClassRoom().getClassId();
        return enrollmentRepository.findByClassRoom_ClassIdAndStudent_UserId(classId, studentId)
                .map(e -> "ACTIVE".equals(e.getStatus()))
                .orElse(false);
    }

    public List<PracticeSession> getSessionsForStudent(Long studentId) {
        return practiceSessionRepository.findByStudent_UserIdOrderByCreatedAtDesc(studentId);
    }

    public Optional<PracticeSession> getSession(Long studentId, Long sessionId) {
        Optional<PracticeSession> sessionOpt = practiceSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            PracticeSession session = sessionOpt.get();
            if (session.getStudent().getUserId().equals(studentId)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public List<PracticeQuestion> getQuestionsForSession(Long sessionId) {
        return practiceQuestionRepository.findByPracticeSession_PracticeSessionId(sessionId);
    }

    @Transactional
    public PracticeSession generatePracticeSession(
            Long studentId,
            Long contentId,
            MultipartFile uploadedFile,
            String pastedText,
            String sourceUrl,
            String customPrompt,
            String difficulty,
            String questionType,
            Integer numberOfQuestions
    ) {
        // Validate that at least one source is provided
        boolean hasSource = (contentId != null)
                || (uploadedFile != null && !uploadedFile.isEmpty())
                || (pastedText != null && !pastedText.trim().isEmpty())
                || (sourceUrl != null && !sourceUrl.trim().isEmpty())
                || (customPrompt != null && !customPrompt.trim().isEmpty());
        if (!hasSource) {
            throw new IllegalArgumentException("You must provide at least one study material source or custom prompt instructions.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        String sourceType = "CUSTOM_PROMPT";
        String sourceTitle = "AI Prompt Instructions";
        String sourceText = "";
        String uploadedFilePath = null;
        String originalFileName = null;
        ContentResource resource = null;

        // Process source input
        if (uploadedFile != null && !uploadedFile.isEmpty()) {
            sourceType = "UPLOADED_FILE";
            originalFileName = uploadedFile.getOriginalFilename();
            sourceTitle = originalFileName != null ? originalFileName : "Uploaded File";
            sourceText = fileTextExtractionService.extractText(uploadedFile);

            // Save file physically
            String uploadDir = "uploads/ai-practice";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String uniqueName = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + originalFileName;
            java.io.File dest = new java.io.File(dir, uniqueName);
            try {
                uploadedFile.transferTo(dest);
                uploadedFilePath = dest.getPath().replace("\\", "/");
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save uploaded file: " + e.getMessage(), e);
            }
        } else if (pastedText != null && !pastedText.trim().isEmpty()) {
            sourceType = "PASTED_TEXT";
            sourceTitle = "Pasted Lesson Text";
            sourceText = pastedText.trim();
        } else if (sourceUrl != null && !sourceUrl.trim().isEmpty()) {
            sourceType = "SOURCE_URL";
            sourceTitle = sourceUrl;
            sourceText = "Web resource content from: " + sourceUrl;
        } else if (contentId != null) {
            sourceType = "SELECTED_CONTENT";
            resource = contentResourceRepository.findById(contentId)
                    .orElseThrow(() -> new IllegalArgumentException("Content resource not found."));
            
            if (!isStudentEnrolledInContentClass(studentId, resource)) {
                throw new IllegalArgumentException("Student is not enrolled in the class for this content.");
            }
            
            sourceTitle = resource.getTitle();
            sourceText = resource.getDescription() != null ? resource.getDescription() : resource.getTitle();
        } else if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            sourceType = "CUSTOM_PROMPT";
            sourceTitle = "Custom Prompt Instructions";
            sourceText = customPrompt;
        }

        // Set default practice parameters
        int finalCount = numberOfQuestions != null ? numberOfQuestions : 5;
        String finalDifficulty = difficulty != null ? difficulty : "MEDIUM";
        String finalQuestionType = questionType != null ? questionType : "MCQ";
        String focusTopic = null;
        boolean shortestCorrect = false;
        boolean longestCorrect = false;

        // Parse custom instructions
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            String cleanPrompt = customPrompt.toLowerCase();

            // Parse count
            java.util.regex.Pattern pCount = java.util.regex.Pattern.compile("\\b(10|[1-9])\\b");
            java.util.regex.Matcher mCount = pCount.matcher(cleanPrompt);
            if (mCount.find()) {
                finalCount = Integer.parseInt(mCount.group(1));
            } else {
                if (cleanPrompt.contains("một") || cleanPrompt.contains("one")) finalCount = 1;
                else if (cleanPrompt.contains("hai") || cleanPrompt.contains("two")) finalCount = 2;
                else if (cleanPrompt.contains("ba") || cleanPrompt.contains("three")) finalCount = 3;
                else if (cleanPrompt.contains("bốn") || cleanPrompt.contains("four")) finalCount = 4;
                else if (cleanPrompt.contains("năm") || cleanPrompt.contains("five")) finalCount = 5;
                else if (cleanPrompt.contains("sáu") || cleanPrompt.contains("six")) finalCount = 6;
                else if (cleanPrompt.contains("bảy") || cleanPrompt.contains("seven")) finalCount = 7;
                else if (cleanPrompt.contains("tám") || cleanPrompt.contains("eight")) finalCount = 8;
                else if (cleanPrompt.contains("chín") || cleanPrompt.contains("nine")) finalCount = 9;
                else if (cleanPrompt.contains("mười") || cleanPrompt.contains("ten")) finalCount = 10;
            }

            // Parse difficulty
            if (cleanPrompt.contains("dễ") || cleanPrompt.contains("easy")) {
                finalDifficulty = "EASY";
            } else if (cleanPrompt.contains("khó") || cleanPrompt.contains("hard")) {
                finalDifficulty = "HARD";
            } else if (cleanPrompt.contains("trung bình") || cleanPrompt.contains("medium") || cleanPrompt.contains("vừa")) {
                finalDifficulty = "MEDIUM";
            }

            // Parse question type
            if (cleanPrompt.contains("trắc nghiệm") || cleanPrompt.contains("mcq") || cleanPrompt.contains("multiple choice") || cleanPrompt.contains("nhiều lựa chọn")) {
                finalQuestionType = "MCQ";
            } else if (cleanPrompt.contains("đúng sai") || cleanPrompt.contains("true false") || cleanPrompt.contains("true/false") || cleanPrompt.contains("t/f")) {
                finalQuestionType = "TRUE_FALSE";
            } else if (cleanPrompt.contains("tự luận") || cleanPrompt.contains("essay") || cleanPrompt.contains("trả lời ngắn") || cleanPrompt.contains("short answer")) {
                finalQuestionType = "SHORT_ANSWER";
            }

            // Parse focus topic
            String[] focusKeywords = {"focus on", "tập trung vào", "about"};
            for (String kw : focusKeywords) {
                int idx = cleanPrompt.indexOf(kw);
                if (idx != -1) {
                    String sub = customPrompt.substring(idx + kw.length()).trim();
                    int endIdx = sub.indexOf('.');
                    if (endIdx == -1) endIdx = sub.indexOf(',');
                    if (endIdx == -1) endIdx = Math.min(sub.length(), 40);
                    if (endIdx > 0) {
                        focusTopic = sub.substring(0, endIdx).trim();
                    }
                    break;
                }
            }

            // Parse answer styles
            if (cleanPrompt.contains("ngắn nhất") || cleanPrompt.contains("shortest")) {
                shortestCorrect = true;
            } else if (cleanPrompt.contains("dài nhất") || cleanPrompt.contains("longest")) {
                longestCorrect = true;
            }
        }

        // Auto-detect focus topic if not set
        if (focusTopic == null) {
            String combinedText = (sourceTitle + " " + sourceText).toLowerCase();
            if (combinedText.contains("constructivism") || combinedText.contains("constructivist") || combinedText.contains("kiến tạo")) {
                focusTopic = "social constructivism";
            } else if (combinedText.contains("flutter") || combinedText.contains("dart") || combinedText.contains("mobile") || combinedText.contains("widget")) {
                focusTopic = "Flutter";
            } else if (combinedText.contains("use case") || combinedText.contains("analysis model") || combinedText.contains("requirement") || combinedText.contains("software")) {
                focusTopic = "Use Case Diagrams";
            }
        }

        // Limit count
        if (finalCount < 1) finalCount = 1;
        if (finalCount > 10) finalCount = 10;

        // Build and save session
        PracticeSession session = PracticeSession.builder()
                .student(student)
                .content(resource)
                .sourceType(sourceType)
                .sourceTitle(sourceTitle)
                .sourceText(sourceText)
                .sourceUrl(sourceUrl)
                .uploadedFilePath(uploadedFilePath)
                .originalFileName(originalFileName)
                .customPrompt(customPrompt)
                .difficulty(finalDifficulty.toUpperCase())
                .questionType(finalQuestionType.toUpperCase())
                .totalQuestions(finalCount)
                .correctAnswers(0)
                .build();

        session = practiceSessionRepository.save(session);

        // Generate questions
        for (int i = 0; i < finalCount; i++) {
            GeneratedQuestionData data = generateQuestionForTopic(focusTopic, finalQuestionType, i, shortestCorrect, longestCorrect, sourceTitle);
            
            PracticeQuestion question = PracticeQuestion.builder()
                    .practiceSession(session)
                    .questionContent(data.questionContent)
                    .questionType(data.questionType.toUpperCase())
                    .optionA(data.optionA)
                    .optionB(data.optionB)
                    .optionC(data.optionC)
                    .optionD(data.optionD)
                    .correctAnswer(data.correctAnswer)
                    .explanation(data.explanation)
                    .build();

            practiceQuestionRepository.save(question);
        }

        // Save progress log under enrolled classroom if available
        LearningNode progressNode = null;
        if (resource != null) {
            progressNode = resource.getLearningNode();
        } else {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent_UserIdAndStatus(studentId, "ACTIVE");
            if (!enrollments.isEmpty()) {
                ClassRoom cr = enrollments.get(0).getClassRoom();
                List<LearningNode> nodes = learningNodeRepository.findByClassRoom_ClassIdAndVisibleTrueOrderByOrderIndexAsc(cr.getClassId());
                if (!nodes.isEmpty()) {
                    progressNode = nodes.get(0);
                }
            }
        }

        if (progressNode != null) {
            LearningProgressLog log = LearningProgressLog.builder()
                    .student(student)
                    .classRoom(progressNode.getClassRoom())
                    .learningNode(progressNode)
                    .activityType("TAKE_PRACTICE")
                    .progressPercent(BigDecimal.valueOf(100.00))
                    .build();
            learningProgressLogRepository.save(log);
        }

        return session;
    }

    private GeneratedQuestionData generateQuestionForTopic(String topic, String questionType, int index, boolean shortestCorrect, boolean longestCorrect, String title) {
        GeneratedQuestionData data = new GeneratedQuestionData();
        data.questionType = questionType;

        String cleanTopic = topic != null ? topic.toLowerCase().trim() : "default";

        if ("social constructivism".equals(cleanTopic)) {
            if ("MCQ".equalsIgnoreCase(questionType)) {
                data.correctAnswer = "A";
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Who is the primary theorist associated with Social Constructivism?";
                        data.optionA = "Lev Vygotsky";
                        data.optionB = "Jean Piaget";
                        data.optionC = "B.F. Skinner";
                        data.optionD = "John Watson";
                        data.explanation = "Lev Vygotsky developed the Socio-cultural Theory of learning, which formed the basis of Social Constructivism.";
                        break;
                    case 1:
                        data.questionContent = "What does 'ZPD' stand for in the context of cognitive and social development?";
                        data.optionA = "Zone of Proximal Development";
                        data.optionB = "Zero Point Division";
                        data.optionC = "Zone of Phase Development";
                        data.optionD = "Zone of Personal Discovery";
                        data.explanation = "ZPD stands for Zone of Proximal Development, defining the range of learning guided by a peer or teacher.";
                        break;
                    case 2:
                        data.questionContent = "In a social constructivist classroom, how is the role of the teacher defined?";
                        data.optionA = "As a facilitator of learning";
                        data.optionB = "As the sole source of knowledge";
                        data.optionC = "As a strict behavior evaluator";
                        data.optionD = "As a quiet passive observer";
                        data.explanation = "The teacher acts as a facilitator, guiding discussions and scaffolded interactions.";
                        break;
                    default:
                        data.questionContent = "Which method is most aligned with social constructivism?";
                        data.optionA = "Collaborative group project";
                        data.optionB = "Rote memorization drills";
                        data.optionC = "Individual silent exams";
                        data.optionD = "Passive lecture listening";
                        data.explanation = "Collaborative projects align with social constructivism by building knowledge via peer interaction.";
                        break;
                }
            } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Social constructivism claims that learning is primarily an individual, passive cognitive activity.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "Constructivism asserts that learning is an active, collaborative process of constructing meaning.";
                        break;
                    case 1:
                        data.questionContent = "Vygotsky's scaffolding theory involves gradually fading teacher support as students gain autonomy.";
                        data.correctAnswer = "A"; // True
                        data.explanation = "Scaffolding provides temporary support and is faded out as the learner masters the skill.";
                        break;
                    case 2:
                        data.questionContent = "Language is the primary psychological tool for social constructivist learning.";
                        data.correctAnswer = "A"; // True
                        data.explanation = "Language plays a central role in mediating social interaction and internalizing knowledge.";
                        break;
                    default:
                        data.questionContent = "Social constructivist classrooms focus purely on standardized exams rather than real-world tasks.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "Constructivist learning relies on authentic tasks and situated learning scenarios.";
                        break;
                }
            } else { // SHORT_ANSWER
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Explain Vygotsky's Zone of Proximal Development (ZPD).";
                        data.correctAnswer = "The range between a learner's independent performance and what they can achieve with knowledgeable guidance.";
                        data.explanation = "ZPD is where learning takes place via social collaboration.";
                        break;
                    case 1:
                        data.questionContent = "Briefly describe the facilitator role of a constructivist teacher.";
                        data.correctAnswer = "The teacher guides conversations, poses challenging questions, and provides scaffolds rather than lecturing.";
                        data.explanation = "This supports students in discovering concepts themselves.";
                        break;
                    case 2:
                        data.questionContent = "What is the importance of collaborative learning in constructivism?";
                        data.correctAnswer = "It enables peers to negotiate meaning, share perspectives, and co-construct knowledge.";
                        data.explanation = "Collaboration forces cognitive conflict and deeper synthesis.";
                        break;
                    default:
                        data.questionContent = "Give one example of educational scaffolding.";
                        data.correctAnswer = "Providing graphic organizers, model templates, or guided prompts that are faded later.";
                        data.explanation = "Scaffolding guides learners towards independence.";
                        break;
                }
            }
        } else if ("flutter".equals(cleanTopic)) {
            if ("MCQ".equalsIgnoreCase(questionType)) {
                data.correctAnswer = "A";
                switch (index % 4) {
                    case 0:
                        data.questionContent = "What is the primary programming language used to build Flutter apps?";
                        data.optionA = "Dart";
                        data.optionB = "Java";
                        data.optionC = "Swift";
                        data.optionD = "Kotlin";
                        data.explanation = "Flutter uses Dart because of its sub-second stateful hot reload and native performance.";
                        break;
                    case 1:
                        data.questionContent = "Which layout widget arranges its children in a vertical array?";
                        data.optionA = "Column";
                        data.optionB = "Row";
                        data.optionC = "Stack";
                        data.optionD = "Wrap";
                        data.explanation = "A Column widget displays its children vertically; a Row widget displays them horizontally.";
                        break;
                    case 2:
                        data.questionContent = "Which method is called to update the UI of a StatefulWidget?";
                        data.optionA = "setState()";
                        data.optionB = "initState()";
                        data.optionC = "build()";
                        data.optionD = "dispose()";
                        data.explanation = "setState() notifies the framework to rebuild the widget and refresh the UI.";
                        break;
                    default:
                        data.questionContent = "What is the entry point function of a Flutter application?";
                        data.optionA = "main()";
                        data.optionB = "runApp()";
                        data.optionC = "createState()";
                        data.optionD = "initState()";
                        data.explanation = "All Dart/Flutter programs begin execution at the main() function.";
                        break;
                }
            } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Flutter widgets are immutable, meaning they cannot be modified once created.";
                        data.correctAnswer = "A"; // True
                        data.explanation = "All Widget instances in Flutter are annotated with @immutable and rebuilt when state changes.";
                        break;
                    case 1:
                        data.questionContent = "Flutter relies on native OS UI components like WebViews to render buttons and text.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "Flutter renders every pixel of its UI directly onto a Skia or Impeller graphics canvas.";
                        break;
                    case 2:
                        data.questionContent = "StatelessWidget can hold dynamic state variables that update the screen.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "StatelessWidgets have no internal mutable state. You must use StatefulWidget for mutable states.";
                        break;
                    default:
                        data.questionContent = "Hot Reload resets the application state and clears all current page inputs.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "Hot Reload injects code updates directly into the Dart VM, preserving the current state.";
                        break;
                }
            } else { // SHORT_ANSWER
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Explain the difference between StatelessWidget and StatefulWidget in Flutter.";
                        data.correctAnswer = "StatelessWidgets are static and immutable, whereas StatefullWidgets maintain a mutable State object that can rebuild the UI.";
                        data.explanation = "Stateless is for content; Stateful is for user interaction.";
                        break;
                    case 1:
                        data.questionContent = "What is the role of the pubspec.yaml file in a Flutter project?";
                        data.correctAnswer = "It manages project dependencies, assets, fonts, and metadata for the Flutter application.";
                        data.explanation = "It serves as the package configuration file.";
                        break;
                    case 2:
                        data.questionContent = "What does the build() method do in a Flutter widget?";
                        data.correctAnswer = "It describes and constructs the visual widget tree to render on the screen.";
                        data.explanation = "build() is called when the widget is inserted or state changes.";
                        break;
                    default:
                        data.questionContent = "Describe what 'Hot Reload' is in Flutter.";
                        data.correctAnswer = "It compiles and injects updated Dart code into the running VM in less than a second, maintaining app state.";
                        data.explanation = "It speeds up building user interfaces and debugging.";
                        break;
                }
            }
        } else if ("use case diagrams".equals(cleanTopic)) {
            if ("MCQ".equalsIgnoreCase(questionType)) {
                data.correctAnswer = "A";
                switch (index % 4) {
                    case 0:
                        data.questionContent = "What is the primary purpose of a Use Case Diagram?";
                        data.optionA = "Show system interactions";
                        data.optionB = "Model database schemas";
                        data.optionC = "Represent class methods";
                        data.optionD = "Describe hardware servers";
                        data.explanation = "Use Case diagrams capture functional requirements by showing interactions between actors and use cases.";
                        break;
                    case 1:
                        data.questionContent = "In a Use Case Diagram, what does a stick figure represent?";
                        data.optionA = "User or system role";
                        data.optionB = "A database table";
                        data.optionC = "A specific code class";
                        data.optionD = "A project manager";
                        data.explanation = "An actor represents a role (human or system) that interacts with the system.";
                        break;
                    case 2:
                        data.questionContent = "Which relationship type indicates mandatory sub-behavior?";
                        data.optionA = "«include»";
                        data.optionB = "«extend»";
                        data.optionC = "Generalization";
                        data.optionD = "Association";
                        data.explanation = "The include relationship models behavior that is always executed as part of the base use case.";
                        break;
                    default:
                        data.questionContent = "What is the main objective of requirement analysis?";
                        data.optionA = "Understand user needs";
                        data.optionB = "Write database queries";
                        data.optionC = "Deploy to AWS";
                        data.optionD = "Create visual CSS styles";
                        data.explanation = "Requirement analysis is used to discover, identify, and document stakeholder needs.";
                        break;
                }
            } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                switch (index % 4) {
                    case 0:
                        data.questionContent = "A Use Case diagram shows the internal algorithms and sequential logic of a system.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "Use Case diagrams capture external interactions, not internal algorithms.";
                        break;
                    case 1:
                        data.questionContent = "An actor in a Use Case Diagram can be an external hardware device or another software system.";
                        data.correctAnswer = "A"; // True
                        data.explanation = "Actors represent any entity outside the system boundary that interacts with it.";
                        break;
                    case 2:
                        data.questionContent = "Non-functional requirements specify constraints on services, such as security and performance.";
                        data.correctAnswer = "A"; // True
                        data.explanation = "Non-functional requirements describe quality attributes and operational constraints.";
                        break;
                    default:
                        data.questionContent = "Functional requirements describe the physical deployment environment of the server.";
                        data.correctAnswer = "B"; // False
                        data.explanation = "Functional requirements define what the system must do, not how it is deployed.";
                        break;
                }
            } else { // SHORT_ANSWER
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Explain the difference between include and extend relationships in Use Case Diagrams.";
                        data.correctAnswer = "Include is for mandatory behavior that always runs, whereas extend is for optional or conditional behavior.";
                        data.explanation = "Include reuse is automatic; extend is conditional.";
                        break;
                    case 1:
                        data.questionContent = "What is requirement elicitation in software engineering?";
                        data.correctAnswer = "The process of gathering software requirements from stakeholders via interviews, workshops, or surveys.";
                        data.explanation = "It is the first step in understanding system requirements.";
                        break;
                    case 2:
                        data.questionContent = "Why is it important to draw a system boundary in a Use Case Diagram?";
                        data.correctAnswer = "It clearly defines what is inside the system (use cases) versus what is outside (actors), establishing scope.";
                        data.explanation = "Defining scope prevents project creep.";
                        break;
                    default:
                        data.questionContent = "What is the difference between functional and non-functional requirements?";
                        data.correctAnswer = "Functional requirements define features (what the system does), while non-functional requirements define attributes like security and speed (how it performs).";
                        data.explanation = "Both are critical for successful software development.";
                        break;
                }
            }
        } else {
            if ("MCQ".equalsIgnoreCase(questionType)) {
                data.correctAnswer = "A";
                switch (index % 4) {
                    case 0:
                        data.questionContent = "What is the main idea of '" + title + "'?";
                        data.optionA = "The main concept of the selected learning material.";
                        data.optionB = "An unrelated minor detail.";
                        data.optionC = "A generic placeholder concept.";
                        data.optionD = "None of the above.";
                        data.explanation = "This question helps students review the key concept before class.";
                        break;
                    case 1:
                        data.questionContent = "Which of the following best describes the scope of '" + title + "'?";
                        data.optionA = "High-level overview and fundamental concepts.";
                        data.optionB = "Advanced implementation details only.";
                        data.optionC = "Administrative instructions.";
                        data.optionD = "Historical background only.";
                        data.explanation = "Flipped classroom material covers foundational topics to prepare students.";
                        break;
                    case 2:
                        data.questionContent = "To get the most out of '" + title + "', students should:";
                        data.optionA = "Read and study it carefully before class.";
                        data.optionB = "Ignore it until the final exam.";
                        data.optionC = "Copy notes from a classmate.";
                        data.optionD = "Skip class after reading.";
                        data.explanation = "Studying pre-class materials is essential for active classroom discussions.";
                        break;
                    default:
                        data.questionContent = "Under what topic does '" + title + "' fall?";
                        data.optionA = "Core curriculum topics.";
                        data.optionB = "Elective reading.";
                        data.optionC = "Extracurricular activity.";
                        data.optionD = "None of the above.";
                        data.explanation = "This resource is mapped to the official learning path nodes.";
                        break;
                }
            } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                switch (index % 4) {
                    case 0:
                        data.questionContent = "The material '" + title + "' is used for pre-class learning.\n\nA) True\nB) False";
                        data.correctAnswer = "A";
                        data.explanation = "In flipped classroom, students study materials before class.";
                        break;
                    case 1:
                        data.questionContent = "Studying '" + title + "' is completely optional and has no effect on classroom discussion quality.\n\nA) True\nB) False";
                        data.correctAnswer = "B";
                        data.explanation = "Pre-class preparation directly impacts the depth of in-class collaboration.";
                        break;
                    case 2:
                        data.questionContent = "The content resource '" + title + "' is visible to enrolled students.\n\nA) True\nB) False";
                        data.correctAnswer = "A";
                        data.explanation = "All visible learning node resources are accessible by enrolled students.";
                        break;
                    default:
                        data.questionContent = "You should read or watch '" + title + "' only after the course has finished.\n\nA) True\nB) False";
                        data.correctAnswer = "B";
                        data.explanation = "Materials should be reviewed prior to class sessions.";
                        break;
                }
            } else {
                switch (index % 4) {
                    case 0:
                        data.questionContent = "Briefly explain what you learned from '" + title + "'.";
                        data.correctAnswer = "Student should mention the main concept of the material.";
                        data.explanation = "This question checks understanding of the learning resource.";
                        break;
                    case 1:
                        data.questionContent = "State one key takeaway from '" + title + "'.";
                        data.correctAnswer = "Any relevant takeaway from the content.";
                        data.explanation = "Reflecting on takeaways helps consolidate knowledge.";
                        break;
                    case 2:
                        data.questionContent = "How does the material '" + title + "' relate to your course project?";
                        data.correctAnswer = "It provides the conceptual foundation or templates.";
                        data.explanation = "Applying concepts to projects is a key learning goal.";
                        break;
                    default:
                        data.questionContent = "Define the primary target audience for '" + title + "'.";
                        data.correctAnswer = "Students enrolled in this class.";
                        data.explanation = "Resources are tailored for the class level and objectives.";
                        break;
                }
            }
        }

        // Apply prompt constraints (shortestCorrect / longestCorrect)
        if ("MCQ".equalsIgnoreCase(questionType)) {
            if (shortestCorrect) {
                data.correctAnswer = "A";
                data.optionA = shortenString(data.optionA, 15);
                data.optionB = lengthenString(data.optionB, 60);
                data.optionC = lengthenString(data.optionC, 60);
                data.optionD = lengthenString(data.optionD, 60);
            } else if (longestCorrect) {
                data.correctAnswer = "A";
                data.optionA = lengthenString(data.optionA, 100);
                data.optionB = shortenString(data.optionB, 15);
                data.optionC = shortenString(data.optionC, 15);
                data.optionD = shortenString(data.optionD, 15);
            }
        }

        return data;
    }

    private String shortenString(String s, int maxLen) {
        if (s == null) return "Concept.";
        if (s.length() > maxLen) {
            String sub = s.substring(0, maxLen).trim();
            return sub.endsWith(".") ? sub : sub + ".";
        }
        return s;
    }

    private String lengthenString(String s, int minLen) {
        if (s == null) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < minLen) {
            sb.append(" This option serves as a verbose distractor to provide comprehensive choice alternatives for learning purposes.");
        }
        return sb.toString();
    }

    @Transactional
    public PracticeSession submitAnswers(Long studentId, Long sessionId, Map<Long, String> answers) {
        PracticeSession session = getSession(studentId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found or does not belong to student."));

        List<PracticeQuestion> questions = getQuestionsForSession(sessionId);
        int correctCount = 0;

        for (PracticeQuestion q : questions) {
            String ans = answers.get(q.getPracticeQuestionId());
            if (ans != null) {
                ans = ans.trim();
            } else {
                ans = "";
            }

            q.setStudentAnswer(ans);
            boolean correct = false;

            if ("MCQ".equalsIgnoreCase(q.getQuestionType()) || "TRUE_FALSE".equalsIgnoreCase(q.getQuestionType())) {
                correct = q.getCorrectAnswer().equalsIgnoreCase(ans);
            } else { // SHORT_ANSWER
                correct = !ans.isEmpty() && ans.length() >= 5; // Mark correct if not empty and >= 5 characters
            }

            q.setIsCorrect(correct);
            if (correct) {
                correctCount++;
            }
            practiceQuestionRepository.save(q);
        }

        session.setCorrectAnswers(correctCount);
        return practiceSessionRepository.save(session);
    }

    private static class GeneratedQuestionData {
        String questionContent;
        String questionType;
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        String correctAnswer;
        String explanation;
    }
}
