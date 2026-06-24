package com.example.fcms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FcmsViewController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam(defaultValue = "STUDENT") String role) {
        if ("TEACHER".equalsIgnoreCase(role)) {
            return "redirect:/teacher/dashboard";
        }
        return "redirect:/student/learning-path";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam(defaultValue = "STUDENT") String role) {
        if ("TEACHER".equalsIgnoreCase(role)) {
            return "redirect:/teacher/dashboard";
        }
        return "redirect:/student/learning-path";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "redirect:/student/learning-path";
    }

    @GetMapping("/student/courses")
    public String studentCourses() {
        return "redirect:/student/learning-path";
    }

    @GetMapping("/student/assignment")
    public String assignmentSubmissionRedirect() {
        return "redirect:/student/assignments";
    }

    @GetMapping("/student/ai-assistant")
    public String aiAssistantRedirect() {
        return "redirect:/student/ai-practice";
    }

    @GetMapping("/teacher/classes")
    public String teacherClasses() {
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/teacher/question-bank")
    public String questionBank() {
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/teacher/progress")
    public String teacherProgress() {
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard(Model model) {
        model.addAttribute("teacherName", "Prof. Tran Phuong");
        model.addAttribute("semester", "Fall 2025");
        model.addAttribute("activeClassCount", 6);
        model.addAttribute("totalStudents", 238);
        model.addAttribute("avgCompletion", "48%");

        model.addAttribute("classes", List.of(
                new ClassRoomCard(1L, "Software Engineering", "Fall 2025", "SE2024", 38, "2h ago", 64, "#1a73e8"),
                new ClassRoomCard(2L, "Data Structures & Algorithms", "Fall 2025", "DSA301", 45, "Yesterday", 42, "#f26522"),
                new ClassRoomCard(3L, "Web Development Fundamentals", "Fall 2025", "WDF102", 52, "3d ago", 78, "#34a853")
        ));

        return "teacher-dashboard";
    }

    public static class ClassRoomCard {
        private final Long id;
        private final String name;
        private final String semester;
        private final String classCode;
        private final int studentCount;
        private final String updatedText;
        private final int completion;
        private final String color;

        public ClassRoomCard(Long id, String name, String semester, String classCode,
                             int studentCount, String updatedText, int completion, String color) {
            this.id = id;
            this.name = name;
            this.semester = semester;
            this.classCode = classCode;
            this.studentCount = studentCount;
            this.updatedText = updatedText;
            this.completion = completion;
            this.color = color;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getSemester() { return semester; }
        public String getClassCode() { return classCode; }
        public int getStudentCount() { return studentCount; }
        public String getUpdatedText() { return updatedText; }
        public int getCompletion() { return completion; }
        public String getColor() { return color; }
    }

    public static class CourseInfo {
        private final String name;
        private final String classCode;
        private final String teacherName;
        private final String semester;
        private final int studentCount;

        public CourseInfo(String name, String classCode, String teacherName, String semester, int studentCount) {
            this.name = name;
            this.classCode = classCode;
            this.teacherName = teacherName;
            this.semester = semester;
            this.studentCount = studentCount;
        }

        public String getName() { return name; }
        public String getClassCode() { return classCode; }
        public String getTeacherName() { return teacherName; }
        public String getSemester() { return semester; }
        public int getStudentCount() { return studentCount; }
    }

    public static class TopicView {
        private final Long id;
        private final String title;
        private final String status;
        private final boolean completed;
        private final int completedNodes;
        private final int totalNodes;
        private final int progress;
        private final List<NodeView> nodes;

        public TopicView(Long id, String title, String status, boolean completed,
                         int completedNodes, int totalNodes, int progress, List<NodeView> nodes) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.completed = completed;
            this.completedNodes = completedNodes;
            this.totalNodes = totalNodes;
            this.progress = progress;
            this.nodes = nodes;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public boolean isCompleted() { return completed; }
        public int getCompletedNodes() { return completedNodes; }
        public int getTotalNodes() { return totalNodes; }
        public int getProgress() { return progress; }
        public List<NodeView> getNodes() { return nodes; }
    }

    public static class NodeView {
        private final String title;
        private final String type;
        private final String duration;
        private final String deadline;
        private final String icon;
        private final String status;

        public NodeView(String title, String type, String duration, String deadline, String icon, String status) {
            this.title = title;
            this.type = type;
            this.duration = duration;
            this.deadline = deadline;
            this.icon = icon;
            this.status = status;
        }

        public String getTitle() { return title; }
        public String getType() { return type; }
        public String getDuration() { return duration; }
        public String getDeadline() { return deadline; }
        public String getIcon() { return icon; }
        public String getStatus() { return status; }
    }

    public static class ChatMessage {
        private final String role;
        private final String text;
        private final String time;

        public ChatMessage(String role, String text, String time) {
            this.role = role;
            this.text = text;
            this.time = time;
        }

        public String getRole() { return role; }
        public String getText() { return text; }
        public String getTime() { return time; }
    }

    public static class AssignmentView {
        private final Long id;
        private final String status;
        private final String title;
        private final String deadlineText;
        private final String maxScore;
        private final String description;

        public AssignmentView(Long id, String status, String title, String deadlineText, String maxScore, String description) {
            this.id = id;
            this.status = status;
            this.title = title;
            this.deadlineText = deadlineText;
            this.maxScore = maxScore;
            this.description = description;
        }

        public Long getId() { return id; }
        public String getStatus() { return status; }
        public String getTitle() { return title; }
        public String getDeadlineText() { return deadlineText; }
        public String getMaxScore() { return maxScore; }
        public String getDescription() { return description; }
    }
}
