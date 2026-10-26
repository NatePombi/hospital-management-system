package com.nathan.hospitalmanagementsystem.department.repository;

import com.nathan.hospitalmanagementsystem.department.entity.Department;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import support.PostgresTestConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
public class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager  entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_shouldPersistAndReloadDepartment(){
        Department department = Department.create("Cardiologist","Heart-related consultations");

        Department saved = departmentRepository.saveAndFlush(department);

        Long id = saved.getId();
        assertNotNull(id);

        entityManager.clear();

        Department reloaded = departmentRepository.findById(id).get();
        assertNotNull(reloaded);



        assertAll(
                ()-> assertEquals(id,reloaded.getId()),
                ()-> assertEquals("Cardiologist",reloaded.getName()),
                ()-> assertEquals("Heart-related consultations",reloaded.getDescription()),
                ()-> assertTrue(reloaded.isActive()),
                ()-> assertNotNull(reloaded.getCreatedAt())
        );


    }

    @Test
    void existsByNameIgnoreCase_shouldFindNameIgnoreCase (){
        saveDepartment("Neurosurgeon");

        boolean exists = departmentRepository.existsByNameIgnoreCase("Neurosurgeon");

        assertTrue(exists);
    }

    @Test
    void existsByNameIgnoreCase_shouldNotFindNameIgnoreCase(){
        saveDepartment("Neurosurgeon");

        boolean exists = departmentRepository.existsByNameIgnoreCase("Cardiologist");

        assertFalse(exists);
    }

    @Test
    void existsByNameIgnoreCaseAndIdNot_shouldFailToFindNameIgnoreCaseAndIdNot(){
        Department department = saveDepartment("Neurosurgeon");

        boolean exists = departmentRepository.existsByNameIgnoreCaseAndIdNot("Neurosurgeon",department.getId());

        assertFalse(exists);

    }

    @Test
    void existsByNameIgnoreCaseAndIdNot_shouldFindNameIgnoreCaseAndIdNot(){
        Department department = saveDepartment("Neurosurgeon");
        saveDepartment("Cardiology");

        boolean exists = departmentRepository.existsByNameIgnoreCaseAndIdNot("Cardiology",department.getId());

        assertTrue(exists);

    }

    @Test
    void findByActiveTrue_shouldReturnOnlyActiveDepartmentsAcrossPages(){
        saveDepartment("Neurology");
        saveDepartment("Cardiology");
        saveDepartment("Dermatology");

        jdbcTemplate.update(
                "INSERT INTO departments (name,active) values(?,?)",
                "Administration",
                false
        );

        entityManager.clear();

        Sort sort = Sort.by("name").ascending().and(Sort.by("id")).ascending();

        Page<Department> firstPage = departmentRepository.findByActiveTrue(PageRequest.of(0,2,sort));
        Page<Department>  secondPage = departmentRepository.findByActiveTrue(PageRequest.of(1,2,sort));

        assertAll(
                ()-> assertEquals(
                        List.of("Cardiology","Dermatology"),
                        firstPage.getContent().stream()
                                .map(Department::getName)
                                .toList()
                ),
                ()-> assertEquals(3L, firstPage.getTotalElements()),
                ()-> assertEquals(2, firstPage.getTotalPages()),
                ()-> assertTrue(firstPage.hasNext()),
                ()-> assertEquals(
                        List.of("Neurology"),
                        secondPage.getContent().stream()
                                .map(Department::getName)
                                .toList()
                ),
                ()-> assertEquals(3L, secondPage.getTotalElements()),
                ()-> assertFalse(secondPage.hasNext())
        );



    }

    @Test
    void findByActiveTrue_shouldReturnEmptyPageWhenAllDepartmentsAreInActive() {
        jdbcTemplate.update(
                "INSERT INTO departments(name, active) VALUES(?,?)",
                "Administration",
                false
        );

        Page<Department> firstPage = departmentRepository.findByActiveTrue(PageRequest.of(0,2));

        assertAll(
                ()-> assertTrue(firstPage.isEmpty()),
                ()-> assertEquals(0,firstPage.getTotalElements())
        );
    }

    @Test
    void save_shouldRejectDuplicateNameIgnoringCase(){
        saveDepartment("cardiOlogY");

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> {
            saveDepartment("Cardiology");
        });


        assertTrue(ex.getMostSpecificCause().getMessage().contains("ux_departments_name_lower"));
    }

    @Test
    void database_shouldRejectBlankDepartmentName(){
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO departments(name) VALUES(?)",
                    "  "
            );
        });

        assertTrue(ex.getMostSpecificCause().getMessage().contains("chk_departments_name_not_blank"));
    }




    private Department saveDepartment(String name){
        return departmentRepository.save(Department.create(name,null));
    }
}
