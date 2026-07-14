package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class ContentController {

    private final LearningNodeRepository learningNodeRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final LearningProgressLogRepository learningProgressLogRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "docx", "pptx", "zip", "png", "jpg", "jpeg", "gif");

    public ContentController(LearningNodeRepository learningNodeRepository,
                             ContentResourceRepository contentResourceRepository,
                             LearningProgressLogRepository learningProgressLogRepository,
                             UserRepository userRepository) {
        this.learningNodeRepository = learningNodeRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.learningProgressLogRepository = learningProgressLogRepository;
        this.userRepository = userRepository;
    }

    private User getLoggedInTeacher(HttpSession session) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId == null) {
            return userRepository.findAll().stream()
                    .filter(u -> "TEACHER".equals(u.getRole()))
                    .findFirst()
                    .orElse(null);
        }
        return userRepository.findById(userId).orElse(null);
    }

    private User getLoggedInStudent(HttpSession session) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId == null) {
            return userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equals(u.getRole()))
                    .findFirst()
                    .orElse(null);
        }
        return userRepository.findById(userId).orElse(null);
    }

    // 1. Render Manage Content Page
    @GetMapping("/teacher/topics/{nodeId}/contents/manage")
    public String manageContents(@PathVariable Long nodeId, Model model, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }

        LearningNode node = learningNodeRepository.findById(nodeId).orElse(null);
        if (node == null || !node.getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        List<ContentResource> contents = contentResourceRepository.findByLearningNodeNodeId(nodeId);

        model.addAttribute("topic", node);
        model.addAttribute("classRoom", node.getClassRoom());
        model.addAttribute("contents", contents);
        model.addAttribute("teacherName", teacher.getFullName());

        return "manage-content";
    }

    // 2. Add Content (Upload file or add URL)
    @PostMapping("/teacher/topics/{nodeId}/contents")
    public String addContent(@PathVariable Long nodeId,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam String contentType,
                             @RequestParam(required = false) String externalUrl,
                             @RequestParam(required = false) MultipartFile file,
                             HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        LearningNode node = learningNodeRepository.findById(nodeId).orElse(null);
        if (node == null || teacher == null || !node.getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        if (title == null || title.trim().isEmpty()) {
            return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=EmptyTitle";
        }

        ContentResource.ContentResourceBuilder builder = ContentResource.builder()
                .learningNode(node)
                .uploadedBy(teacher)
                .title(title.trim())
                .description(description)
                .contentType(contentType)
                .visible(true);

        if ("FILE".equalsIgnoreCase(contentType)) {
            if (file == null || file.isEmpty()) {
                return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=NoFileSelected";
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=FileSizeExceeded";
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=InvalidFileType";
            }

            try {
                // Ensure uploads directory exists
                File uploadDirFile = new File(UPLOAD_DIR);
                if (!uploadDirFile.exists()) {
                    uploadDirFile.mkdirs();
                }

                // Generate unique file name
                String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
                Path destinationPath = Paths.get(UPLOAD_DIR, uniqueFilename);
                Files.copy(file.getInputStream(), destinationPath);

                // Save resource details
                builder.filePath("/uploads/" + uniqueFilename)
                        .originalFileName(originalFilename)
                        .fileSize(file.getSize())
                        .fileMimeType(file.getContentType());

            } catch (IOException e) {
                return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=FileUploadFailed";
            }
        } else {
            // URL content type validation
            if (externalUrl == null || !externalUrl.toLowerCase().startsWith("http")) {
                return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=InvalidUrl";
            }
            builder.externalUrl(externalUrl.trim());
        }

        contentResourceRepository.save(builder.build());

        return "redirect:/teacher/topics/" + nodeId + "/contents/manage?success=ContentAdded";
    }

    // 3. Delete Content Resource
    @PostMapping("/teacher/contents/{contentId}/delete")
    public String deleteContent(@PathVariable Long contentId, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        ContentResource content = contentResourceRepository.findById(contentId).orElse(null);
        if (content == null || teacher == null || 
            !content.getLearningNode().getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        Long nodeId = content.getLearningNode().getNodeId();

        // Cascade delete referencing records in other tables to avoid foreign key violations
        learningProgressLogRepository.deleteLogsByContentId(contentId);
        contentResourceRepository.deletePracticeQuestionsByContentId(contentId);
        contentResourceRepository.deletePracticeSessionsByContentId(contentId);

        // If content is file, delete physical file first
        if ("FILE".equalsIgnoreCase(content.getContentType()) && content.getFilePath() != null) {
            try {
                String localPath = content.getFilePath().replace("/uploads/", UPLOAD_DIR + "/");
                File file = new File(localPath);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                System.err.println("Failed to delete physical file: " + content.getFilePath() + " - " + e.getMessage());
            }
        }

        contentResourceRepository.delete(content);

        return "redirect:/teacher/topics/" + nodeId + "/contents/manage?success=ContentDeleted";
    }

    // 4. Toggle Content Visibility
    @PostMapping("/teacher/contents/{contentId}/toggle-visibility")
    public String toggleContentVisibility(@PathVariable Long contentId, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        ContentResource content = contentResourceRepository.findById(contentId).orElse(null);
        if (content == null || teacher == null || 
            !content.getLearningNode().getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        Long nodeId = content.getLearningNode().getNodeId();
        content.setVisible(!content.getVisible());
        contentResourceRepository.save(content);

        return "redirect:/teacher/topics/" + nodeId + "/contents/manage?success=VisibilityToggled";
    }

    // 5. Student View Content & Log Learning Progress
    @GetMapping("/student/contents/{contentId}/view")
    public String studentViewContent(@PathVariable Long contentId, HttpSession session) {
        User student = getLoggedInStudent(session);
        if (student == null) {
            return "redirect:/login";
        }

        ContentResource content = contentResourceRepository.findById(contentId).orElse(null);
        if (content == null || !content.getVisible()) {
            return "redirect:/student/learning-path?error=ContentNotFound";
        }

        LearningNode node = content.getLearningNode();
        ClassRoom classRoom = node.getClassRoom();

        // Write progress log
        String activityType = "OPEN_LINK";
        if ("FILE".equalsIgnoreCase(content.getContentType())) {
            activityType = "VIEW_CONTENT";
        } else if ("VIDEO_URL".equalsIgnoreCase(content.getContentType())) {
            activityType = "WATCH_VIDEO";
        }

        LearningProgressLog log = LearningProgressLog.builder()
                .student(student)
                .classRoom(classRoom)
                .learningNode(node)
                .content(content)
                .activityType(activityType)
                .progressPercent(BigDecimal.valueOf(100.00))
                .build();
        
        learningProgressLogRepository.save(log);

        // Redirect based on type
        if ("FILE".equalsIgnoreCase(content.getContentType())) {
            return "redirect:" + content.getFilePath();
        } else {
            return "redirect:" + content.getExternalUrl();
        }
    }

    // 6. Edit Content Resource
    @PostMapping("/teacher/contents/{contentId}/edit")
    public String editContent(@PathVariable Long contentId,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String externalUrl,
                              @RequestParam(required = false) MultipartFile file,
                              HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        ContentResource content = contentResourceRepository.findById(contentId).orElse(null);
        if (content == null || teacher == null || 
            !content.getLearningNode().getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        Long nodeId = content.getLearningNode().getNodeId();

        if (title == null || title.trim().isEmpty()) {
            return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=EmptyTitle";
        }

        content.setTitle(title.trim());
        content.setDescription(description);

        if ("FILE".equalsIgnoreCase(content.getContentType())) {
            // Check if a new file is uploaded to replace the old one
            if (file != null && !file.isEmpty()) {
                if (file.getSize() > MAX_FILE_SIZE) {
                    return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=FileSizeExceeded";
                }

                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                }

                if (!ALLOWED_EXTENSIONS.contains(extension)) {
                    return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=InvalidFileType";
                }

                try {
                    // Delete old physical file first
                    if (content.getFilePath() != null) {
                        String oldLocalPath = content.getFilePath().replace("/uploads/", UPLOAD_DIR + "/");
                        File oldFile = new File(oldLocalPath);
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                    }

                    // Save new physical file
                    String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
                    Path destinationPath = Paths.get(UPLOAD_DIR, uniqueFilename);
                    Files.copy(file.getInputStream(), destinationPath);

                    // Update details
                    content.setFilePath("/uploads/" + uniqueFilename);
                    content.setOriginalFileName(originalFilename);
                    content.setFileSize(file.getSize());
                    content.setFileMimeType(file.getContentType());

                } catch (IOException e) {
                    return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=FileUploadFailed";
                }
            }
        } else {
            // URL content type validation
            if (externalUrl == null || !externalUrl.toLowerCase().startsWith("http")) {
                return "redirect:/teacher/topics/" + nodeId + "/contents/manage?error=InvalidUrl";
            }
            content.setExternalUrl(externalUrl.trim());
        }

        contentResourceRepository.save(content);

        return "redirect:/teacher/topics/" + nodeId + "/contents/manage?success=ContentUpdated";
    }
}
