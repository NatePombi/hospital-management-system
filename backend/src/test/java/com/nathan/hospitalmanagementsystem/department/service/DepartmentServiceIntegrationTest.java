package com.nathan.hospitalmanagementsystem.department.service;

import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.dto.UpdateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.entity.Department;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.repository.DepartmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import support.PostgresTestConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
public class DepartmentServiceIntegrationTest {
    @Autowired
    private  DepartmentService departmentService;
    @MockitoSpyBean
    private DepartmentRepository departmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp(){
        departmentRepository.deleteAll();
    }

    @Test
    void updateDepartment_shouldPersistDetailsAndPreserveMetadata() {
        Department saved = saveDepartment(
                "Cardiology",
                "Heart consultations"
        );

        Long id = saved.getId();

        jdbcTemplate.update(
                "UPDATE departments SET active = false WHERE id = ?",
                id
        );

        Department original = departmentRepository.findById(id).orElseThrow();
        LocalDateTime originalCreatedAt = original.getCreatedAt();

        DepartmentResponse response = departmentService.updateDepartmentById(
                id,
                new UpdateDepartmentRequest(
                        "  Advanced Cardiology  ",
                        "  Specialist heart consultations  "
                )
        );

        Department reloaded = departmentRepository.findById(id).orElseThrow();

        assertAll(
                () -> assertEquals(id, reloaded.getId()),
                () -> assertEquals(
                        "Advanced Cardiology",
                        reloaded.getName()
                ),
                () -> assertEquals(
                        "Specialist heart consultations",
                        reloaded.getDescription()
                ),
                () -> assertFalse(reloaded.getActive()),
                () -> assertEquals(1L, departmentRepository.count())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cardiology", "CARDIOLOGY"})
    void updateDepartment_shouldAllowOwnNameRegardlessOfCase(
            String name
    ) {
        Department saved = saveDepartment(
                "Cardiology",
                "Heart consultations"
        );

        DepartmentResponse response = departmentService.updateDepartmentById(
                saved.getId(),
                new UpdateDepartmentRequest(name, "Updated description")
        );

        Department reloaded = departmentRepository.findById(saved.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(name, response.name()),
                () -> assertEquals(name, reloaded.getName()),
                () -> assertEquals(
                        "Updated description",
                        reloaded.getDescription()
                ),
                () -> assertEquals(1L, departmentRepository.count())
        );
    }

    @Test
    void updateDepartment_shouldPersistClearedDescription() {
        Department saved = saveDepartment(
                "Cardiology",
                "Heart consultations"
        );

        DepartmentResponse response = departmentService.updateDepartmentById(
                saved.getId(),
                new UpdateDepartmentRequest("Cardiology", null)
        );

        Department reloaded = departmentRepository.findById(saved.getId())
                .orElseThrow();

        assertAll(
                () -> assertNull(response.description()),
                () -> assertNull(reloaded.getDescription()),
                () -> assertEquals("Cardiology", reloaded.getName())
        );
    }

    @Test
    void updateDepartment_shouldPreserveRecordsWhenDuplicateIsDetected() {
        Department cardiology = saveDepartment(
                "Cardiology",
                "Heart consultations"
        );

        Department neurology = saveDepartment(
                "Neurology",
                "Brain consultations"
        );

        assertThrows(
                DepartmentAlreadyExistsException.class,
                () -> departmentService.updateDepartmentById(
                        cardiology.getId(),
                        new UpdateDepartmentRequest(
                                "NEUROLOGY",
                                "Changed description"
                        )
                )
        );

        Department unchangedCardiology =
                departmentRepository.findById(cardiology.getId()).orElseThrow();

        Department unchangedNeurology =
                departmentRepository.findById(neurology.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(
                        "Cardiology",
                        unchangedCardiology.getName()
                ),
                () -> assertEquals(
                        "Heart consultations",
                        unchangedCardiology.getDescription()
                ),
                () -> assertEquals(
                        "Neurology",
                        unchangedNeurology.getName()
                ),
                () -> assertEquals(
                        "Brain consultations",
                        unchangedNeurology.getDescription()
                ),
                () -> assertEquals(2L, departmentRepository.count())
        );
    }

    @Test
    void updateDepartment_shouldRollBackWhenDatabaseRejectsDuplicateName() {
        Department cardiology = saveDepartment(
                "Cardiology",
                "Heart consultations"
        );

        Department neurology = saveDepartment(
                "Neurology",
                "Brain consultations"
        );

        doReturn(false).when(departmentRepository)
                .existsByNameIgnoreCaseAndIdNot(
                        "NEUROLOGY",
                        cardiology.getId()
                );

        DepartmentAlreadyExistsException exception = assertThrows(
                DepartmentAlreadyExistsException.class,
                () -> departmentService.updateDepartmentById(
                        cardiology.getId(),
                        new UpdateDepartmentRequest(
                                "NEUROLOGY",
                                "Changed description"
                        )
                )
        );

        DataIntegrityViolationException databaseFailure = assertInstanceOf(
                DataIntegrityViolationException.class,
                exception.getCause()
        );

        assertTrue(
                databaseFailure.getMostSpecificCause().getMessage()
                        .contains("ux_departments_name_lower")
        );

        Department unchangedCardiology =
                departmentRepository.findById(cardiology.getId()).orElseThrow();

        Department unchangedNeurology =
                departmentRepository.findById(neurology.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(
                        "Cardiology",
                        unchangedCardiology.getName()
                ),
                () -> assertEquals(
                        "Heart consultations",
                        unchangedCardiology.getDescription()
                ),
                () -> assertEquals(
                        "Neurology",
                        unchangedNeurology.getName()
                ),
                () -> assertEquals(
                        "Brain consultations",
                        unchangedNeurology.getDescription()
                ),
                () -> assertEquals(2L, departmentRepository.count())
        );
    }

    private Department saveDepartment(String name, String description) {
        return departmentRepository.saveAndFlush(
                Department.create(name, description)
        );
    }


}
