package com.example.fcms.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class JoinClassRequest {

    @NotBlank(message = "Class code is required")
    @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "Class code must be 6 letters or numbers")
    private String classCode;

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }
}
