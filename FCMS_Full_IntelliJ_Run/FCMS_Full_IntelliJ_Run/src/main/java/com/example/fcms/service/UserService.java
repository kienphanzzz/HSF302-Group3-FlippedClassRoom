package com.example.fcms.service;

import com.example.fcms.dto.user.UpdateProfileRequest;
import com.example.fcms.entity.User;
import com.example.fcms.repository.UserRepository;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {


    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp"
    );

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

        applyProfileFields(user, request);
        user.setAvatarUrl(cleanText(request.getAvatarUrl()));

        return userRepository.save(user);
    }


    public User updateProfile(Long currentUserId, UpdateProfileRequest request, MultipartFile avatarFile) {
        User user = getCurrentUser(currentUserId);

        applyProfileFields(user, request);
        if (avatarFile != null && !avatarFile.isEmpty()) {
            user.setAvatarUrl(storeAvatar(avatarFile));
        } else {
            user.setAvatarUrl(cleanText(request.getAvatarUrl()));
        }

        return userRepository.save(user);
    }

    private void applyProfileFields(User user, UpdateProfileRequest request) {
        user.setFullName(request.getFullName().trim());
        user.setPhone(cleanText(request.getPhone()));
        user.setBio(cleanText(request.getBio()));
    }

    private String storeAvatar(MultipartFile avatarFile) {
        if (avatarFile.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar image must be 5MB or smaller.");
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Please upload a JPG, PNG, GIF, or WEBP image.");
        }

        String extension = avatarExtension(avatarFile);
        if (!extensionMatchesContentType(extension, contentType)) {
            throw new IllegalArgumentException("Avatar file extension does not match the image type.");
        }

        try {
            byte[] fileBytes = avatarFile.getBytes();
            if (!hasValidImageSignature(fileBytes, contentType)) {
                throw new IllegalArgumentException("Avatar file content is not a valid image.");
            }

            Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String fileName = UUID.randomUUID() + "." + extension;
            Path targetPath = uploadDir.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid avatar file.");
            }

            Files.write(targetPath, fileBytes);
            return "/uploads/avatars/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save avatar image.");
        }
    }

    private String avatarExtension(MultipartFile avatarFile) {
        String fileName = avatarFile.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Avatar image must have a valid file extension.");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Avatar image must be a JPG, PNG, GIF, or WEBP file.");
        }

        return extension.equals("jpeg") ? "jpg" : extension;
    }

    private boolean extensionMatchesContentType(String extension, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> extension.equals("jpg");
            case "image/png" -> extension.equals("png");
            case "image/gif" -> extension.equals("gif");
            case "image/webp" -> extension.equals("webp");
            default -> false;
        };
    }

    private boolean hasValidImageSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/gif" -> startsWith(bytes, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(bytes, 'G', 'I', 'F', '8', '9', 'a');
            case "image/webp" -> startsWith(bytes, 'R', 'I', 'F', 'F')
                    && bytes.length >= 12
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... expectedBytes) {
        if (bytes.length < expectedBytes.length) {
            return false;
        }

        for (int index = 0; index < expectedBytes.length; index++) {
            if ((bytes[index] & 0xFF) != expectedBytes[index]) {
                return false;
            }
        }

        return true;
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
