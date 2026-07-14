package com.example.fcms.controller;

import com.example.fcms.entity.*;
import com.example.fcms.repository.*;
import com.example.fcms.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/teacher")
public class TeacherActionController {

    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherQuestionService teacherQuestionService;

    public TeacherActionController(UserRepository userRepository,
                                   ClassRoomRepository classRoomRepository,
                                   TeacherQuestionService teacherQuestionService) {
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.teacherQuestionService = teacherQuestionService;
    }

    private User getLoggedInTeacher(HttpSession session) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    // 1. Toggle class status (Archive / Unarchive)
    @PostMapping("/classes/{classId}/toggle-archive")
    public String toggleArchiveClass(@PathVariable Long classId, HttpSession session, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }

        ClassRoom classRoom = classRoomRepository.findById(classId).orElse(null);
        if (classRoom == null || !classRoom.getTeacher().getUserId().equals(teacher.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Class not found or unauthorized.");
            return "redirect:/teacher/classes";
        }

        if ("ACTIVE".equals(classRoom.getStatus())) {
            classRoom.setStatus("ARCHIVED");
            redirectAttributes.addFlashAttribute("successMessage", "Class archived successfully!");
        } else {
            classRoom.setStatus("ACTIVE");
            redirectAttributes.addFlashAttribute("successMessage", "Class restored to active successfully!");
        }
        classRoomRepository.save(classRoom);

        return "redirect:/teacher/classes";
    }

    // 2. Create question manually
    @PostMapping("/question-bank/create")
    public String createQuestion(@RequestParam String questionContent,
                                 @RequestParam String questionType,
                                 @RequestParam String difficulty,
                                 @RequestParam(required = false) String correctAnswer,
                                 @RequestParam(required = false) String explanation,
                                 @RequestParam(required = false) Long nodeId,
                                 @RequestParam(required = false) List<String> options,
                                 @RequestParam(required = false) Integer correctOptionIndex,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }

        if (questionContent == null || questionContent.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Question content cannot be empty.");
            return "redirect:/teacher/question-bank";
        }

        teacherQuestionService.createQuestion(teacher, questionContent, questionType, difficulty, 
                                              correctAnswer, explanation, nodeId, options, correctOptionIndex);

        redirectAttributes.addFlashAttribute("successMessage", "Question added to bank successfully!");
        return "redirect:/teacher/question-bank";
    }

    // 3. Delete question
    @PostMapping("/question-bank/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long questionId, HttpSession session, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }

        teacherQuestionService.deleteQuestion(teacher.getUserId(), questionId);

        redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully!");
        return "redirect:/teacher/question-bank";
    }

    // 4. Generate AI Questions
    @PostMapping("/question-bank/generate")
    public String generateAiQuestions(@RequestParam(required = false) Long nodeId,
                                      @RequestParam(required = false) String topicName,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInTeacher(session);
        if (teacher == null) {
            return "redirect:/login";
        }

        teacherQuestionService.generateAiQuestions(teacher, nodeId, topicName);

        redirectAttributes.addFlashAttribute("successMessage", "AI generated 3 new questions based on the topic!");
        return "redirect:/teacher/question-bank";
    }
}
