package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import com.example.fcms.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

@Controller
public class ViewController {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final TeacherQuestionService teacherQuestionService;
    private final TeacherProgressService teacherProgressService;

    public ViewController(UserRepository userRepository,
                          ClassRoomRepository classRoomRepository,
                          LearningNodeRepository learningNodeRepository,
                          TeacherQuestionService teacherQuestionService,
                          TeacherProgressService teacherProgressService) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.learningNodeRepository = learningNodeRepository;
        this.teacherQuestionService = teacherQuestionService;
        this.teacherProgressService = teacherProgressService;
    }

    private User getLoggedInTeacher(HttpSession session) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/select-role")
    public String selectRolePage(HttpSession session, Model model) {
        String pendingEmail = (String) session.getAttribute("pendingGoogleEmail");

        if (pendingEmail == null) {
            return "redirect:/login";
        }

        model.addAttribute("pendingEmail", pendingEmail);
        model.addAttribute("pendingName", session.getAttribute("pendingGoogleName"));
        model.addAttribute("pendingAvatar", session.getAttribute("pendingGoogleAvatar"));

        return "select-role";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ─── Student Portal Redirects ──────────────────────────────────────────
    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "redirect:/dashboard";
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

    // ─── Teacher Portal Mappings ──────────────────────────────────────────
    @GetMapping("/teacher/classes")
    public String teacherClasses(Model model, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }
        model.addAttribute("title", "My Classes");
        model.addAttribute("activePage", "classes");
        model.addAttribute("currentUser", teacher);

        List<ClassRoom> activeClasses = classRoomRepository.findByTeacherAndStatus(teacher, "ACTIVE");
        List<ClassRoom> archivedClasses = classRoomRepository.findByTeacherAndStatus(teacher, "ARCHIVED");

        model.addAttribute("activeClasses", activeClasses);
        model.addAttribute("archivedClasses", archivedClasses);

        return "teacher/classes";
    }

    @GetMapping("/teacher/question-bank")
    public String questionBank(Model model, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Question Bank");
        model.addAttribute("activePage", "question-bank");
        model.addAttribute("currentUser", teacher);

        List<Question> questions = teacherQuestionService.getQuestionsWithOptions(teacher.getUserId());
        model.addAttribute("questions", questions);

        // Fetch topics for modal drop-down selection
        List<LearningNode> topics = learningNodeRepository.findByClassRoom_Teacher_UserId(teacher.getUserId());
        model.addAttribute("topics", topics);

        return "teacher/question-bank";
    }

    @GetMapping("/teacher/progress")
    public String teacherProgress(@RequestParam(required = false) Long classId, Model model, HttpSession session) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Student Progress");
        model.addAttribute("activePage", "progress");
        model.addAttribute("currentUser", teacher);

        List<ClassRoom> classes = classRoomRepository.findByTeacherAndStatus(teacher, "ACTIVE");
        model.addAttribute("classes", classes);

        if (classId == null && !classes.isEmpty()) {
            classId = classes.get(0).getClassId();
        }

        if (classId != null) {
            model.addAttribute("selectedClassId", classId);
            List<TeacherProgressService.StudentProgressDTO> progressList = teacherProgressService.getStudentsProgress(classId);
            model.addAttribute("progressList", progressList);

            int totalStudents = progressList.size();
            long atRiskCount = progressList.stream().filter(p -> p.isAtRisk()).count();
            double avgPrep = totalStudents == 0 ? 0.0 : progressList.stream().mapToDouble(p -> p.getPrepRate()).average().orElse(0.0);

            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("atRiskCount", atRiskCount);
            model.addAttribute("avgPrep", avgPrep);
        } else {
            model.addAttribute("progressList", Collections.emptyList());
            model.addAttribute("totalStudents", 0);
            model.addAttribute("atRiskCount", 0);
            model.addAttribute("avgPrep", 0.0);
        }

        return "teacher/progress";
    }

    @GetMapping("/teacher/dashboard")
    public String teacherDashboardRedirect() {
        return "redirect:/dashboard";
    }
}
