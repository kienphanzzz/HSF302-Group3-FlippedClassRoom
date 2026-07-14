package com.example.fcms.service;

import com.example.fcms.entity.Notification;
import com.example.fcms.entity.User;
import com.example.fcms.repository.NotificationRepository;
import com.example.fcms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        List<Notification> notifs = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        if (notifs.isEmpty()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                Notification n1 = Notification.builder()
                        .user(user)
                        .title("Welcome New Member!")
                        .message("Welcome to Flipped Classroom (FCMS). We hope you have a great learning experience here!")
                        .isRead(false)
                        .build();
                notificationRepository.save(n1);

                Notification n2 = Notification.builder()
                        .user(user)
                        .title("Study Reminder")
                        .message("You have new learning materials in your Software Engineering class. Click on My Learning Path to check them out.")
                        .isRead(false)
                        .build();
                notificationRepository.save(n2);
                
                notifs = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
            }
        }
        return notifs;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifs = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        for (Notification notif : notifs) {
            if (!notif.getIsRead()) {
                notif.setIsRead(true);
                notificationRepository.save(notif);
            }
        }
    }
}
