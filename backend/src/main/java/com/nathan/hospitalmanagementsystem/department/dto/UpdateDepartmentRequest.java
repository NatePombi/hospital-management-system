package com.nathan.hospitalmanagementsystem.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotBlank(message = "Department Name cannot be empty")
        @Size(
                max = 100,
                message = "Department name cannot exceed 100 characters"
        )
        String name,

        @Size(
                max = 500,
                message = "Description cannot exceed 500 characters"
        )
        String description
) {
        public UpdateDepartmentRequest{
                if(name != null){
                        name = name.strip();
                }

                if(description != null){
                        description = description.strip();
                }
        }
}
