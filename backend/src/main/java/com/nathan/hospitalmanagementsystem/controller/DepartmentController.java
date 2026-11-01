package com.nathan.hospitalmanagementsystem.controller;

import com.nathan.hospitalmanagementsystem.common.dto.PaginatedResponse;
import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.service.DepartmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/departments")
@Validated
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request){
        DepartmentResponse response = departmentService.createDepartment(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/fetch/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable @Positive(message = "Department ID must be greater than zero") Long id){
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping("/fetchAll")
    public ResponseEntity<PaginatedResponse<DepartmentResponse>> getDepartments(@RequestParam(defaultValue = "0")
                                                                                @Min(value = 0, message = "Page must be at least zero")
                                                                                int page,

                                                                                @RequestParam(defaultValue = "5")
                                                                                @Min(value = 1,message = "Page size must be at least 1")
                                                                                @Max(value = 100, message = "Page size cannot exceed 100")
                                                                                int size,

                                                                                @RequestParam(name = "activeOnly",defaultValue = "true")
                                                                                boolean activeOnly
                                                                                ){
        return ResponseEntity.ok().body(departmentService.getDepartments(page, size, activeOnly));
    }

}
