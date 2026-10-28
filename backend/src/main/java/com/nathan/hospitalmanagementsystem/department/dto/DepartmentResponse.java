package com.nathan.hospitalmanagementsystem.department.dto;

import com.nathan.hospitalmanagementsystem.department.entity.Department;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.isActive(),
                department.getCreatedAt()
        );
    }
}
