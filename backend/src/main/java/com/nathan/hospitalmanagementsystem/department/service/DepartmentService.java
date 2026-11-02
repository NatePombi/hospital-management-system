package com.nathan.hospitalmanagementsystem.department.service;

import com.nathan.hospitalmanagementsystem.common.dto.PaginatedResponse;
import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.dto.UpdateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.entity.Department;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentNotFoundException;
import com.nathan.hospitalmanagementsystem.department.repository.DepartmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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


    @Transactional
    public DepartmentResponse getDepartmentById(Long id){
        return departmentRepository.findById(id)
                .map(DepartmentResponse::from)
                .orElseThrow(()-> new DepartmentNotFoundException(id));
    }


    @Transactional
    public PaginatedResponse<DepartmentResponse> getDepartments(int page, int size, boolean activeOnly){
        Sort sort = Sort.by(Sort.Order.asc("name"),
                Sort.Order.asc("id"));


        Pageable pageable = PageRequest.of(page,size,sort);

        Page<Department> departments = activeOnly
                ? departmentRepository.findByActiveTrue(pageable)
                : departmentRepository.findAll(pageable);

        Page<DepartmentResponse> response = departments.map(DepartmentResponse::from);

        return PaginatedResponse.from(response);
    }

    @Transactional
    public DepartmentResponse updateDepartmentById(Long id, UpdateDepartmentRequest request){
        Department department = departmentRepository.findById(id).orElseThrow(()-> new DepartmentNotFoundException(id));

        if(departmentRepository.existsByNameIgnoreCaseAndIdNot(request.name(),id)){
            throw new DepartmentAlreadyExistsException(request.name());
        }

        department.updateDepartment(request.name(),request.description());


        try{
            Department saved = departmentRepository.saveAndFlush(department);
            return DepartmentResponse.from(saved);

        }

        catch (DataIntegrityViolationException ex){
            if(isDuplicateNameViolation(ex)){
                throw new DepartmentAlreadyExistsException(request.name(),ex);
            }

            throw ex;
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
