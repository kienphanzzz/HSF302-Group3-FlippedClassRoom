package com.example.fcms.controller;

import com.example.fcms.entity.ClassRoom;
import com.example.fcms.entity.LearningNode;
import com.example.fcms.entity.ContentResource;
import com.example.fcms.entity.User;
import com.example.fcms.repository.ClassRoomRepository;
import com.example.fcms.repository.ContentResourceRepository;
import com.example.fcms.repository.LearningNodeRepository;
import com.example.fcms.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/teacher/classes")
public class LearningNodeController {

    private final ClassRoomRepository classRoomRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ContentResourceRepository contentResourceRepository;
    private final UserRepository userRepository;

    public LearningNodeController(ClassRoomRepository classRoomRepository,
                                  LearningNodeRepository learningNodeRepository,
                                  ContentResourceRepository contentResourceRepository,
                                  UserRepository userRepository) {
        this.classRoomRepository = classRoomRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.contentResourceRepository = contentResourceRepository;
        this.userRepository = userRepository;
    }

    private User getLoggedInTeacher(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"TEACHER".equals(user.getRole())) {
            // Fallback for testing/standalone execution
            return userRepository.findAll().stream()
                    .filter(u -> "TEACHER".equals(u.getRole()))
                    .findFirst()
                    .orElse(null);
        }
        return user;
    }

    // 1. View Class Details & List Topics (Learning Nodes)
    @GetMapping("/{classId}")
    public String viewClassDetails(@PathVariable Long classId, Model model, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }

        Optional<ClassRoom> classRoomOpt = classRoomRepository.findById(classId);
        if (classRoomOpt.isEmpty()) {
            return "redirect:/teacher/dashboard?error=ClassNotFound";
        }
        ClassRoom classRoom = classRoomOpt.get();

        // Enforce business rule: Teacher can only view/manage their own classes
        if (!classRoom.getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        List<LearningNode> nodes = learningNodeRepository.findByClassRoomClassIdOrderByOrderIndexAsc(classId);
        
        // Map to hold content resources for each topic node
        Map<Long, List<ContentResource>> nodeContents = new HashMap<>();
        for (LearningNode node : nodes) {
            List<ContentResource> contents = contentResourceRepository.findByLearningNodeNodeId(node.getNodeId());
            nodeContents.put(node.getNodeId(), contents);
        }

        model.addAttribute("classRoom", classRoom);
        model.addAttribute("topics", nodes);
        model.addAttribute("nodeContents", nodeContents);
        model.addAttribute("teacherName", teacher.getFullName());
        
        return "topic-detail";
    }

    // 2. Create Topic
    @PostMapping("/{classId}/topics")
    public String createTopic(@PathVariable Long classId,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        ClassRoom classRoom = classRoomRepository.findById(classId).orElse(null);
        if (classRoom == null || teacher == null || !classRoom.getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        if (title == null || title.trim().isEmpty()) {
            return "redirect:/teacher/classes/" + classId + "?error=EmptyTitle";
        }

        // Get max order index to append
        List<LearningNode> existingNodes = learningNodeRepository.findByClassRoomClassIdOrderByOrderIndexAsc(classId);
        int nextOrder = existingNodes.stream()
                .mapToInt(LearningNode::getOrderIndex)
                .max()
                .orElse(0) + 1;

        LearningNode.LearningNodeBuilder builder = LearningNode.builder()
                .classRoom(classRoom)
                .title(title.trim())
                .description(description)
                .orderIndex(nextOrder)
                .visible(true);

        if (startDate != null && !startDate.isEmpty()) {
            builder.suggestedStartDate(LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            builder.suggestedEndDate(LocalDate.parse(endDate));
        }

        learningNodeRepository.save(builder.build());

        return "redirect:/teacher/classes/" + classId + "?success=TopicCreated";
    }

    // 3. Edit Topic
    @PostMapping("/{classId}/topics/{nodeId}/edit")
    public String editTopic(@PathVariable Long classId,
                            @PathVariable Long nodeId,
                            @RequestParam String title,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate,
                            @RequestParam(required = false) Integer orderIndex,
                            HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        LearningNode node = learningNodeRepository.findById(nodeId).orElse(null);
        if (node == null || teacher == null || !node.getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        if (title == null || title.trim().isEmpty()) {
            return "redirect:/teacher/classes/" + classId + "?error=EmptyTitle";
        }

        node.setTitle(title.trim());
        node.setDescription(description);
        if (orderIndex != null) {
            node.setOrderIndex(orderIndex);
        }

        if (startDate != null && !startDate.isEmpty()) {
            node.setSuggestedStartDate(LocalDate.parse(startDate));
        } else {
            node.setSuggestedStartDate(null);
        }

        if (endDate != null && !endDate.isEmpty()) {
            node.setSuggestedEndDate(LocalDate.parse(endDate));
        } else {
            node.setSuggestedEndDate(null);
        }

        learningNodeRepository.save(node);

        return "redirect:/teacher/classes/" + classId + "?success=TopicUpdated";
    }

    // 4. Delete Topic
    @PostMapping("/{classId}/topics/{nodeId}/delete")
    public String deleteTopic(@PathVariable Long classId,
                              @PathVariable Long nodeId,
                              HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        LearningNode node = learningNodeRepository.findById(nodeId).orElse(null);
        if (node == null || teacher == null || !node.getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        // Delete all contents and physical files first
        List<ContentResource> contents = contentResourceRepository.findByLearningNodeNodeId(nodeId);
        for (ContentResource content : contents) {
            if ("FILE".equals(content.getContentType()) && content.getFilePath() != null) {
                try {
                    File file = new File(content.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to delete physical file: " + content.getFilePath() + " - " + e.getMessage());
                }
            }
            contentResourceRepository.delete(content);
        }

        learningNodeRepository.delete(node);

        return "redirect:/teacher/classes/" + classId + "?success=TopicDeleted";
    }

    // 5. Toggle Topic Visibility
    @PostMapping("/{classId}/topics/{nodeId}/toggle-visibility")
    public String toggleTopicVisibility(@PathVariable Long classId,
                                        @PathVariable Long nodeId,
                                        HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        LearningNode node = learningNodeRepository.findById(nodeId).orElse(null);
        if (node == null || teacher == null || !node.getClassRoom().getTeacher().getUserId().equals(teacher.getUserId())) {
            return "redirect:/teacher/dashboard?error=Unauthorized";
        }

        node.setVisible(!node.getVisible());
        learningNodeRepository.save(node);

        return "redirect:/teacher/classes/" + classId + "?success=VisibilityToggled";
    }
}
