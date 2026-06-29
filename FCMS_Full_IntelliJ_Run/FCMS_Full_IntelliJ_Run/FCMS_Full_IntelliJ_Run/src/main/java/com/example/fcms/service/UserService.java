package com.example.fcms.service;

import com.example.fcms.dto.user.UpdateProfileRequest;
import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new SecurityException("Please login first");
        }

        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new SecurityException("Please login first"));
    }

    public User updateProfile(Long currentUserId, UpdateProfileRequest request) {
        User user = getCurrentUser(currentUserId);

        user.setFullName(request.getFullName().trim());
        user.setPhone(cleanText(request.getPhone()));
        user.setBio(cleanText(request.getBio()));
        user.setAvatarUrl(cleanText(request.getAvatarUrl()));

        return userRepository.save(user);
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
