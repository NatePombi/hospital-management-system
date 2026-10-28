package com.nathan.hospitalmanagementsystem.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = "Department name cannot be blank")
        @Size(
                max = 100,
                message = "Department name cannot exceed 100 characters"
        )
        String name,

        @Size(
                max = 500,
                message = "Department description cannot exceed 500 characters"
        )
        String description
) {
    public CreateDepartmentRequest {
        if (name != null) {
            name = name.strip();
        }

        if (description != null) {
            description = description.strip();
        }
    }
}
