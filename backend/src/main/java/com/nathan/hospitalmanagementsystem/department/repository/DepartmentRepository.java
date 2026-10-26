package com.nathan.hospitalmanagementsystem.department.repository;

import com.nathan.hospitalmanagementsystem.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<Department> findByActiveTrue(Pageable pageable);
}
