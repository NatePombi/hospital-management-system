package com.nathan.hospitalmanagementsystem.department.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class DepartmentTest {

    @Test
    void create_shouldTrimValuesAndActivateDepartment(){
        Department department = Department.create(
                "   Cardiologist     ",
                "      Heart-related consultations       ");

        assertAll(
                ()-> assertEquals("Cardiologist",department.getName()),
                ()-> assertEquals("Heart-related consultations",department.getDescription()),
                ()-> assertTrue(department.isActive())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void create_shouldRejectBlankNames(String name){
        assertThrows(IllegalArgumentException.class,()->{
            Department.create(name,"description");
        });
    }

    @Test
    void create_shouldAcceptNAmeExactly100CharactersAfterTrim(){
        String name = "N".repeat(100);

        Department department = Department.create(name,"description");

        assertEquals(name,department.getName());
    }

    @Test
    void create_shouldRejectNameLongerThan100CharactersAfterTrim(){
        String name = "N".repeat(101);

        assertThrows(IllegalArgumentException.class,()->{
            Department.create(name,"description");
        });
    }

    @Test
    void create_shouldAllowNullDescription(){
        Department department = Department.create("Cardiologist",null);

        assertEquals("Cardiologist",department.getName());
        assertNull(department.getDescription());
    }

    @Test
    void create_shouldAcceptDescriptionExactly500CharactersAfterTrim(){
        String desc = "N".repeat(500);

        Department department = Department.create("Cardiologist",desc);
        assertEquals(desc,department.getDescription());
    }


    @Test
    void create_shouldRejectDescriptionLongerThan500CharactersAfterTrim(){
        String desc = "N".repeat(501);
        assertThrows(IllegalArgumentException.class,()->{
            Department.create("Cardiologist",desc);
        });
    }
}
