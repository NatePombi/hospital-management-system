package com.nathan.hospitalmanagementsystem.department.service;

import com.nathan.hospitalmanagementsystem.common.dto.PaginatedResponse;
import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.entity.Department;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentNotFoundException;
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
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private LocalDateTime localDateTime = LocalDateTime.now();



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
        department = Department.create("Neurology","Brain-related symptoms");

        when(departmentRepository.existsByNameIgnoreCase(department.getName())).thenReturn(true);
        CreateDepartmentRequest request = new CreateDepartmentRequest("  Neurology   ","   Brain-related symptoms   ");

        assertThrows(DepartmentAlreadyExistsException.class,()->{
            service.createDepartment(request);
        });


        verify(departmentRepository,never()).saveAndFlush(any(Department.class));
    }

    @Test
    void createDepartment_shouldTranslateDatabaseDuplicateNameFailure(){
         department = Department.create("Neurology","Brain-related symptoms");

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
        department = Department.create("Neurology","Brain-related symptoms");

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
        department = Department.create("Neurology","Brain-related symptoms");

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
        department = Department.create("Neurology","Brain-related symptoms");

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

    @Test
    void getDepartmentById_shouldReturnStoredDepartment(){
        department = storedDepartment(12L,"Neurology",true);
        when(departmentRepository.findById(12L)).thenReturn(Optional.of(department));

        DepartmentResponse response = service.getDepartmentById(12L);

        assertNotNull(response);

        assertAll(
                ()-> assertEquals(12L, response.id()),
                ()-> assertEquals("Neurology", response.name()),
                ()-> assertTrue(response.active()),
                ()-> assertEquals(localDateTime,response.createdAt())
        );
    }

    @Test
    void getDepartmentById_shouldThrowWhenDepartmentNotExist(){
        when(departmentRepository.findById(11L)).thenThrow(new DepartmentNotFoundException(11L));

        assertThrows(DepartmentNotFoundException.class,()->{
            service.getDepartmentById(11L);
        });
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

    @Test
    void getDepartments_shouldReturnActiveDepartmentsWithPagination() {
        Pageable pageable = departmentPage(0, 2);

        Department cardiology = storedDepartment(1L, "Cardiology", true);
        Department dermatology = storedDepartment(2L, "Dermatology", true);

        Page<Department> departments = new PageImpl<>(
                List.of(cardiology, dermatology),
                pageable,
                5
        );

        when(departmentRepository.findByActiveTrue(pageable))
                .thenReturn(departments);

        PaginatedResponse<DepartmentResponse> response =
                service.getDepartments(0, 2, true);

        assertAll(
                () -> assertEquals(
                        List.of("Cardiology", "Dermatology"),
                        response.getData().stream()
                                .map(DepartmentResponse::name)
                                .toList()
                ),
                () -> assertEquals(0, response.getCurrentPage()),
                () -> assertEquals(2, response.getSize()),
                () -> assertEquals(5L, response.getTotalItems()),
                () -> assertEquals(3, response.getTotalPages()),
                () -> assertTrue(response.isFirst()),
                () -> assertFalse(response.isLast())
        );

        verify(departmentRepository).findByActiveTrue(pageable);
        verify(departmentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getDepartments_shouldIncludeInactiveDepartmentsWhenRequested() {
        Pageable pageable = departmentPage(1, 2);

        Department cardiology = storedDepartment(1L, "Cardiology", true);
        Department neurology = storedDepartment(2L, "Neurology", false);

        Page<Department> departments = new PageImpl<>(
                List.of(cardiology, neurology),
                pageable,
                4
        );

        when(departmentRepository.findAll(pageable))
                .thenReturn(departments);

        PaginatedResponse<DepartmentResponse> response =
                service.getDepartments(1, 2, false);

        assertAll(
                () -> assertEquals(
                        List.of("Cardiology", "Neurology"),
                        response.getData().stream()
                                .map(DepartmentResponse::name)
                                .toList()
                ),
                () -> assertEquals(
                        List.of(true, false),
                        response.getData().stream()
                                .map(DepartmentResponse::active)
                                .toList()
                ),
                () -> assertEquals(2, response.getSize()),
                () -> assertEquals(4L, response.getTotalItems()),
                () -> assertEquals(2, response.getTotalPages()),
                () -> assertFalse(response.isFirst()),
                () -> assertTrue(response.isLast())
        );

        verify(departmentRepository).findAll(pageable);
        verify(departmentRepository, never())
                .findByActiveTrue(any(Pageable.class));
    }

    @Test
    void getDepartments_shouldReturnEmptyPageWhenNothingMatches() {
        Pageable pageable = departmentPage(0, 20);

        when(departmentRepository.findByActiveTrue(pageable))
                .thenReturn(Page.empty(pageable));

        PaginatedResponse<DepartmentResponse> response =
                service.getDepartments(0, 20, true);

        assertAll(
                () -> assertTrue(response.getData().isEmpty()),
                () -> assertEquals(0, response.getTotalPages()),
                () -> assertEquals(20, response.getSize()),
                () -> assertEquals(0L, response.getTotalItems()),
                () -> assertEquals(0, response.getTotalPages()),
                () -> assertTrue(response.isFirst()),
                () -> assertTrue(response.isLast())
        );
    }

    @Test
    void getDepartments_shouldPreserveTotalsForPageBeyondLastPage() {
        Pageable pageable = departmentPage(3, 2);

        Page<Department> departments = new PageImpl<>(
                List.of(),
                pageable,
                1
        );

        when(departmentRepository.findByActiveTrue(pageable))
                .thenReturn(departments);

        PaginatedResponse<DepartmentResponse> response =
                service.getDepartments(3, 2, true);

        assertAll(
                () -> assertTrue(response.getData().isEmpty()),
                () -> assertEquals(2, response.getSize()),
                () -> assertEquals(1L, response.getTotalItems()),
                () -> assertEquals(1, response.getTotalPages()),
                () -> assertFalse(response.isFirst()),
                () -> assertTrue(response.isLast())
        );
    }

    private Department storedDepartment(Long id, String name,boolean active){
        Department department1 = mock(Department.class);

        when(department1.getId()).thenReturn(id);
        when(department1.getName()).thenReturn(name);
        when(department1.isActive()).thenReturn(active);
        when(department1.getCreatedAt()).thenReturn(localDateTime);

        return department1;
    }

    private Pageable departmentPage(int page, int size){
        return PageRequest.of(page, size, Sort.by("name").ascending() .and(Sort.by("id").ascending()));
    }
}
