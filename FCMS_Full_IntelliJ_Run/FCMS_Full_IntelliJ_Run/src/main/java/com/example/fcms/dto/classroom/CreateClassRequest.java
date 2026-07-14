package com.example.fcms.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateClassRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 150, message = "Class name max 150 characters")
    private String className;

    @Size(max = 50, message = "Subject code max 50 characters")
    private String subjectCode;

    @Size(max = 2000, message = "Description is too long")
    private String description;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
