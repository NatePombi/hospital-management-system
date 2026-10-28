package com.nathan.hospitalmanagementsystem.department.service;

import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.entity.Department;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.repository.DepartmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private static final String NAME_UNIQUE_INDEX = "ux_departments_name_lower";

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request){

        Department department = Department.create(request.name(), request.description());

        if(departmentRepository.existsByNameIgnoreCase(department.getName())){
            throw new DepartmentAlreadyExistsException(department.getName());
        }

        try {
            Department savedDepartment = departmentRepository.saveAndFlush(department);

            return DepartmentResponse.from(savedDepartment);
        }catch (DataIntegrityViolationException e){
            if(isDuplicateNameViolation(e)){
                throw new DepartmentAlreadyExistsException(department.getName(),e);
            }

            throw e;
        }
    }

    private boolean isDuplicateNameViolation(DataIntegrityViolationException exception){
        Throwable cause = exception;

        while(cause != null){
            if(cause instanceof ConstraintViolationException violation &&
                NAME_UNIQUE_INDEX.equals(violation.getConstraintName())){
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

}
