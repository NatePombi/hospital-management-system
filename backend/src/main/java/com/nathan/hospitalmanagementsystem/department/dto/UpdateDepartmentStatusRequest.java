package com.nathan.hospitalmanagementsystem.department.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDepartmentStatusRequest(
        @NotNull(message = "Active status must be provided")
        Boolean active
) {}
