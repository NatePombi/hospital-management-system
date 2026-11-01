package com.nathan.hospitalmanagementsystem.department.exception;

import com.nathan.hospitalmanagementsystem.common.exception.ResourceNotFoundException;

public class DepartmentNotFoundException extends ResourceNotFoundException {
    public DepartmentNotFoundException(Long id) {
        super("Department not found with ID: " + id);
    }
}
