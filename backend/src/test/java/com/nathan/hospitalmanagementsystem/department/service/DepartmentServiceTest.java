package com.nathan.hospitalmanagementsystem.department.service;

import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.entity.Department;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.repository.DepartmentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {
    @Mock
    private DepartmentRepository departmentRepository;
    @InjectMocks
    private DepartmentService service;

    private Department department;

    @BeforeEach
    void setUp(){
        department = Department.create("Neurology","Brain-related consultation");
    }


    @Test
    void createDepartment_shouldSaveAndReturnResponse(){
        LocalDateTime localDateTime = LocalDateTime.now();

        Department persisted = mock(Department.class);
        when(persisted.getName()).thenReturn("Neurology");
        when(persisted.getId()).thenReturn(14L);
        when(persisted.getDescription()).thenReturn("Brain-related consultation");
        when(persisted.isActive()).thenReturn(true);
        when(persisted.getCreatedAt()).thenReturn(localDateTime);

        when(departmentRepository.existsByNameIgnoreCase("Neurology")).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenReturn(persisted);


        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");

        DepartmentResponse response = service.createDepartment(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);

        verify(departmentRepository).saveAndFlush(captor.capture());

        Department submitted = captor.getValue();

        assertAll(
                ()-> assertEquals(14L,response.id()),
                ()-> assertEquals("Neurology", response.name()),
                ()-> assertEquals("Brain-related consultation",response.description()),
                ()-> assertTrue(response.active()),
                ()-> assertEquals(new DepartmentResponse(
                        14L,
                        "Neurology",
                        "Brain-related consultation",
                        true,
                        localDateTime
                ),response)
        );

    }

    @Test
    void createDepartment_shouldRejectExistingNameBeforeSaving(){
        when(departmentRepository.existsByNameIgnoreCase(department.getName())).thenReturn(true);
        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");

        assertThrows(DepartmentAlreadyExistsException.class,()->{
            service.createDepartment(request);
        });


        verify(departmentRepository,never()).saveAndFlush(any(Department.class));
    }

    @Test
    void createDepartment_shouldTranslateDatabaseDuplicateNameFailure(){
        DataIntegrityViolationException failure = databaseConstraintFailure("ux_departments_name_lower");
        when(departmentRepository.existsByNameIgnoreCase(department.getName())).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(failure);

        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");


        DepartmentAlreadyExistsException ex = assertThrows(DepartmentAlreadyExistsException.class,()->{
            service.createDepartment(request);
        });


        assertSame(failure,ex.getCause());
    }

    @Test
    void createDepartment_shouldPropagateOtherConstraintFailures(){
        DataIntegrityViolationException failure = databaseConstraintFailure("chk_departments_name_not_blank");
        when(departmentRepository.existsByNameIgnoreCase(department.getName())).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(failure);

        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");


        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,()->{
            service.createDepartment(request);
        });

        assertSame(failure,ex);

    }

    @Test
    void createDepartment_shouldPropagateFailuresWithUnknownConstraint(){
        DataIntegrityViolationException failure = databaseConstraintFailure(null);
        when(departmentRepository.existsByNameIgnoreCase(department.getName())).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(failure);

        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");


        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,()->{
            service.createDepartment(request);
        });

        assertSame(failure,ex);

    }

    @Test
    void createDepartment_shouldPropagateIntegrityFailureWithoutCause(){
        DataIntegrityViolationException failure = databaseConstraintFailure("Unknown Cause");
        when(departmentRepository.existsByNameIgnoreCase(department.getName())).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(failure);

        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");


        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,()->{
            service.createDepartment(request);
        });

        assertSame(failure,ex);

    }


    @Test
    void createDepartment_shouldRejectInvalidNameBeforeDatabaseAccess(){
        CreateDepartmentRequest request = new CreateDepartmentRequest("     ","   Brain-related symptoms   ");

        assertThrows(IllegalArgumentException.class,()->{
           service.createDepartment(request);
        });

        verifyNoInteractions(departmentRepository);
    }



    private DataIntegrityViolationException databaseConstraintFailure(String constraintName){
        ConstraintViolationException cause = new ConstraintViolationException(
                "Database constraint violation",
                new SQLException("Constraint violation"),
                constraintName);

        return new DataIntegrityViolationException(
                "Could not insert department",cause
        );
    }
}
