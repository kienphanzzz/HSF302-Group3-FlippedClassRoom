package com.example.fcms.dto.user;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name max 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone max 20 characters")
    @Pattern(regexp = "^$|\\d{10,11}", message = "Phone must contain 10 to 11 digits")
    private String phone;

    @Size(max = 500, message = "Bio max 500 characters")
    private String bio;

    @Size(max = 500, message = "Avatar URL max 500 characters")
    private String avatarUrl;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
