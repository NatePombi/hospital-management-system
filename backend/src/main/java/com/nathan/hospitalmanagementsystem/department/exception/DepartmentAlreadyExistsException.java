package com.nathan.hospitalmanagementsystem.department.exception;

import com.nathan.hospitalmanagementsystem.common.exception.ConflictException;

public class DepartmentAlreadyExistsException extends ConflictException {
    public DepartmentAlreadyExistsException(String name) {
        super("A department named '" + name + "' already exists.");
    }

    public DepartmentAlreadyExistsException(String name, Throwable cause) {
        super("A department named '" + name + "' already exists.", cause);
    }
}
