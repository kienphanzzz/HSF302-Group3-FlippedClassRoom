package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.service.StudentLearningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/student")
public class StudentLearningController {

    private static final Long demoStudentId = 2L;

    private Long getStudentId(HttpSession session) {
        Long studentId = (Long) session.getAttribute("currentUserId");
        if (studentId == null) {
            studentId = demoStudentId;
        }
        return studentId;
    }


    private final StudentLearningService studentLearningService;
    private final com.example.fcms.service.StudentAssignmentService studentAssignmentService;

    public StudentLearningController(StudentLearningService studentLearningService,
                                     com.example.fcms.service.StudentAssignmentService studentAssignmentService) {
        this.studentLearningService = studentLearningService;
        this.studentAssignmentService = studentAssignmentService;
    }

    @GetMapping("/learning-path")
    public String getLearningPath(@RequestParam(required = false) Long classId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        List<ClassRoom> classes = studentLearningService.getJoinedClasses(studentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("classes", classes);
        model.addAttribute("activePage", "classes");

        if (classes.isEmpty()) {
            model.addAttribute("error", "You are not enrolled in any classes.");
            return "student/learning-path";
        }

        // Default to the first class if none specified or if student is not enrolled in the specified class
        ClassRoom activeClass = classes.get(0);
        if (classId != null) {
            Optional<ClassRoom> matchedClass = classes.stream()
                    .filter(c -> c.getClassId().equals(classId))
                    .findFirst();
            if (matchedClass.isPresent()) {
                activeClass = matchedClass.get();
            }
        }

        List<LearningNode> nodes = studentLearningService.getLearningNodes(studentId, activeClass.getClassId());
        
        model.addAttribute("activeClass", activeClass);
        model.addAttribute("nodes", nodes);
        
        // Build lookup maps for nodes
        java.util.Map<Long, List<ContentResource>> nodeResources = new java.util.HashMap<>();
        java.util.Map<Long, List<Assignment>> nodeAssignments = new java.util.HashMap<>();
        for (LearningNode node : nodes) {
            nodeResources.put(node.getNodeId(), studentLearningService.getContentResources(studentId, node.getNodeId()));
            nodeAssignments.put(node.getNodeId(), studentAssignmentService.getAssignmentsForNode(node.getNodeId()));
        }
        model.addAttribute("nodeResources", nodeResources);
        model.addAttribute("nodeAssignments", nodeAssignments);

        // Compute overall progress
        long totalResourcesCount = nodes.stream()
                .mapToLong(node -> studentLearningService.getContentResources(studentId, node.getNodeId()).size())
                .sum();
        long viewedResourcesCount = studentLearningService.getViewedResourcesCountForClass(studentId, activeClass.getClassId());
        int overallProgressValue = 0;
        if (totalResourcesCount > 0) {
            overallProgressValue = (int) ((viewedResourcesCount * 100) / totalResourcesCount);
        }
        
        model.addAttribute("overallProgress", overallProgressValue + "%");
        model.addAttribute("overallProgressValue", overallProgressValue);
        
        return "student/learning-path";
    }

    @GetMapping("/classes")
    public String getClasses(Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        List<ClassRoom> classes = studentLearningService.getJoinedClasses(studentId);
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("classes", classes);
        model.addAttribute("activePage", "classes");
        return "student/classes";
    }

    @GetMapping("/classes/{classId}")
    public String getClassDetail(@PathVariable Long classId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        if (!studentLearningService.isStudentEnrolled(studentId, classId)) {
            return "redirect:/student/classes";
        }
        
        List<ClassRoom> classes = studentLearningService.getJoinedClasses(studentId);
        ClassRoom activeClass = classes.stream()
                .filter(c -> c.getClassId().equals(classId))
                .findFirst()
                .orElseThrow();
        
        List<LearningNode> nodes = studentLearningService.getLearningNodes(studentId, classId);
        
        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("activeClass", activeClass);
        model.addAttribute("nodes", nodes);
        model.addAttribute("activePage", "classes");
        model.addAttribute("classes", classes);
        
        java.util.Map<Long, List<ContentResource>> nodeResources = new java.util.HashMap<>();
        java.util.Map<Long, List<Assignment>> nodeAssignments = new java.util.HashMap<>();
        for (LearningNode node : nodes) {
            nodeResources.put(node.getNodeId(), studentLearningService.getContentResources(studentId, node.getNodeId()));
            nodeAssignments.put(node.getNodeId(), studentAssignmentService.getAssignmentsForNode(node.getNodeId()));
        }
        model.addAttribute("nodeResources", nodeResources);
        model.addAttribute("nodeAssignments", nodeAssignments);

        // Compute overall progress
        long totalResourcesCount = nodes.stream()
                .mapToLong(node -> studentLearningService.getContentResources(studentId, node.getNodeId()).size())
                .sum();
        long viewedResourcesCount = studentLearningService.getViewedResourcesCountForClass(studentId, classId);
        int overallProgressValue = 0;
        if (totalResourcesCount > 0) {
            overallProgressValue = (int) ((viewedResourcesCount * 100) / totalResourcesCount);
        }
        
        model.addAttribute("overallProgress", overallProgressValue + "%");
        model.addAttribute("overallProgressValue", overallProgressValue);
        
        return "student/class-detail";
    }

    @GetMapping("/nodes/{nodeId}")
    public String getNodeDetail(@PathVariable Long nodeId, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        Optional<LearningNode> nodeOpt = studentLearningService.getLearningNode(studentId, nodeId);
        if (nodeOpt.isEmpty()) {
            return "redirect:/student/learning-path";
        }

        LearningNode node = nodeOpt.get();
        List<ContentResource> contents = studentLearningService.getContentResources(studentId, nodeId);

        model.addAttribute("studentName", "Alex Nguyen");
        model.addAttribute("node", node);
        model.addAttribute("contents", contents);
        model.addAttribute("activePage", "classes");
        return "student/node-detail";
    }

    @GetMapping("/content/{contentId}")
    public String viewContent(@PathVariable Long contentId,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        Long studentId = getStudentId(session);
        Optional<ContentResource> contentOpt = studentLearningService.getContentResource(studentId, contentId);
        if (contentOpt.isEmpty()) {
            return "redirect:/student/learning-path";
        }

        // Log the activity progress
        studentLearningService.logProgress(studentId, contentId);

        ContentResource resource = contentOpt.get();
        if (resource.getExternalUrl() != null && !resource.getExternalUrl().isEmpty()) {
            return "redirect:" + resource.getExternalUrl();
        }
        if (resource.getFilePath() != null && !resource.getFilePath().isEmpty()) {
            String localPath = resource.getFilePath().replace("/uploads/", "uploads/");
            java.io.File file = new java.io.File(localPath);
            if (!file.exists()) {
                redirectAttributes.addFlashAttribute("errorMessage", "The file '" + (resource.getOriginalFileName() != null ? resource.getOriginalFileName() : "material") + "' is missing on the server. Please contact your teacher to re-upload it.");
                return "redirect:/student/nodes/" + resource.getLearningNode().getNodeId();
            }
            return "redirect:" + resource.getFilePath();
        }
        
        // If there's no external URL and no file path, show node detail
        return "redirect:/student/nodes/" + resource.getLearningNode().getNodeId();
    }
}
