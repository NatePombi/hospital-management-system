package com.nathan.hospitalmanagementsystem.department.service;

import com.nathan.hospitalmanagementsystem.common.dto.PaginatedResponse;
import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.dto.UpdateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.UpdateDepartmentStatusRequest;
import com.nathan.hospitalmanagementsystem.department.entity.Department;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentNotFoundException;
import com.nathan.hospitalmanagementsystem.department.repository.DepartmentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
        when(persisted.getActive()).thenReturn(true);
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

    @Test
    void updateDepartment_shouldUpdateExistingDepartmentAndReturnResponse() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        UpdateDepartmentRequest request = new UpdateDepartmentRequest(
                "  Advanced Cardiology  ",
                "  Specialist heart consultations  "
        );

        Department persisted = storedDepartment(
                1L,
                "Advanced Cardiology",
                true
        );

        when(persisted.getDescription())
                .thenReturn("Specialist heart consultations");

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.saveAndFlush(department))
                .thenReturn(persisted);

        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Advanced Cardiology", 1L)).thenReturn(false);

        DepartmentResponse response = service.updateDepartmentById(1L, request);

        assertAll(
                () -> assertEquals(
                        "Advanced Cardiology",
                        department.getName()
                ),
                () -> assertEquals(
                        "Specialist heart consultations",
                        department.getDescription()
                ),
                () -> assertTrue(department.getActive()),
                () -> assertEquals(
                        new DepartmentResponse(
                                1L,
                                "Advanced Cardiology",
                                "Specialist heart consultations",
                                true,
                                localDateTime
                        ),
                        response
                )
        );

        verify(departmentRepository).existsByNameIgnoreCaseAndIdNot(
                "Advanced Cardiology",
                1L
        );

        verify(departmentRepository).saveAndFlush(department);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cardiology", "CARDIOLOGY"})
    void updateDepartment_shouldAllowKeepingOwnNameOrChangingItsCase(
            String name
    ) {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest(name, "Updated description");

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.existsByNameIgnoreCaseAndIdNot(name, 1L))
                .thenReturn(false);

        when(departmentRepository.saveAndFlush(department))
                .thenReturn(department);

        DepartmentResponse response = service.updateDepartmentById(1L, request);

        assertAll(
                () -> assertEquals(name, response.name()),
                () -> assertEquals(
                        "Updated description",
                        response.description()
                )
        );

        verify(departmentRepository).existsByNameIgnoreCaseAndIdNot(name, 1L);
        verify(departmentRepository).saveAndFlush(department);
    }

    @Test
    void updateDepartment_shouldClearDescriptionWhenNull() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("Cardiology", null);

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.saveAndFlush(department))
                .thenReturn(department);

        DepartmentResponse response = service.updateDepartmentById(1L, request);

        assertAll(
                () -> assertNull(department.getDescription()),
                () -> assertNull(response.description())
        );

        verify(departmentRepository).saveAndFlush(department);
    }

    @Test
    void updateDepartment_shouldRejectMissingDepartmentBeforeDuplicateCheck() {
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("Cardiology", null);

        when(departmentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                DepartmentNotFoundException.class,
                () -> service.updateDepartmentById(99L, request)
        );

        verify(departmentRepository, never())
                .existsByNameIgnoreCaseAndIdNot(anyString(), anyLong());

        verify(departmentRepository, never())
                .saveAndFlush(any(Department.class));
    }

    @Test
    void updateDepartment_shouldRejectAnotherDepartmentsNameBeforeMutation() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("Neurology", "New description");

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Neurology", 1L))
                .thenReturn(true);

        assertThrows(
                DepartmentAlreadyExistsException.class,
                () -> service.updateDepartmentById(1L, request)
        );

        assertAll(
                () -> assertEquals("Cardiology", department.getName()),
                () -> assertEquals(
                        "Heart consultations",
                        department.getDescription()
                )
        );

        verify(departmentRepository, never())
                .saveAndFlush(any(Department.class));
    }

    @Test
    void updateDepartment_shouldTranslateDatabaseDuplicateNameFailure() {
        Department department = Department.create("Cardiology", null);

        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("Neurology", null);

        DataIntegrityViolationException failure =
                databaseConstraintFailure("ux_departments_name_lower");

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.saveAndFlush(department))
                .thenThrow(failure);

        DepartmentAlreadyExistsException exception = assertThrows(
                DepartmentAlreadyExistsException.class,
                () -> service.updateDepartmentById(1L, request)
        );

        assertSame(failure, exception.getCause());
    }

    @Test
    void updateDepartment_shouldPropagateOtherDatabaseConstraintFailures() {
        Department department = Department.create("Cardiology", null);

        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("Neurology", null);

        DataIntegrityViolationException failure =
                databaseConstraintFailure("chk_departments_name_not_blank");

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.saveAndFlush(department))
                .thenThrow(failure);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.updateDepartmentById(1L, request)
        );

        assertSame(failure, exception);
    }

    @Test
    void updateDepartment_shouldRejectInvalidDetailsWithoutSaving() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        UpdateDepartmentRequest request = new UpdateDepartmentRequest(
                "Neurology",
                "B".repeat(501)
        );

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateDepartmentById(1L, request)
        );

        assertAll(
                () -> assertEquals("Cardiology", department.getName()),
                () -> assertEquals(
                        "Heart consultations",
                        department.getDescription()
                )
        );

        verify(departmentRepository, never())
                .saveAndFlush(any(Department.class));
    }

    @ParameterizedTest
    @CsvSource({
            "true, false",
            "false, true",
            "true, true",
            "false, false"
    })
    void updateDepartmentStatus_shouldApplyRequestedState(
            boolean initiallyActive,
            boolean requestedActive
    ) {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        if (!initiallyActive) {
            department.Deactivate();
        }

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(department));

        when(departmentRepository.saveAndFlush(department))
                .thenReturn(department);

        DepartmentResponse response = service.updateDepartmentStatus(
                1L,
                new UpdateDepartmentStatusRequest(requestedActive)
        );

        assertAll(
                () -> assertEquals(
                        requestedActive,
                        department.getActive()
                ),
                () -> assertEquals(requestedActive, response.active()),
                () -> assertEquals("Cardiology", response.name()),
                () -> assertEquals(
                        "Heart consultations",
                        response.description()
                )
        );

        verify(departmentRepository).saveAndFlush(department);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateDepartmentStatus_shouldRejectMissingDepartment(boolean active) {
        when(departmentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                DepartmentNotFoundException.class,
                () -> service.updateDepartmentStatus(
                        99L,
                        new UpdateDepartmentStatusRequest(active)
                )
        );

        verify(departmentRepository, never())
                .saveAndFlush(any(Department.class));
    }

    @Test
    void updateDepartmentStatus_shouldRejectNullStatusBeforeDatabaseAccess() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateDepartmentStatus(
                        1L,
                        new UpdateDepartmentStatusRequest(null)
                )
        );

        assertEquals(
                "Active status must be provided",
                exception.getMessage()
        );

        verifyNoInteractions(departmentRepository);
    }

    private Department storedDepartment(Long id, String name,boolean active){
        Department department1 = mock(Department.class);

        when(department1.getId()).thenReturn(id);
        when(department1.getName()).thenReturn(name);
        when(department1.getActive()).thenReturn(active);
        when(department1.getCreatedAt()).thenReturn(localDateTime);

        return department1;
    }

    private Pageable departmentPage(int page, int size){
        return PageRequest.of(page, size, Sort.by("name").ascending() .and(Sort.by("id").ascending()));
    }
}
