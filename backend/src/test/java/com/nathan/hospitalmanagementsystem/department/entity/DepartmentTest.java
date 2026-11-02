package com.nathan.hospitalmanagementsystem.department.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

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

    @Test
    void updateDetails_shouldTrimAndUpdateDepartmentDetails() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        department.updateDepartment(
                "  Advanced Cardiology  ",
                "  Specialist heart consultations  "
        );

        assertAll(
                () -> assertEquals(
                        "Advanced Cardiology",
                        department.getName()
                ),
                () -> assertEquals(
                        "Specialist heart consultations",
                        department.getDescription()
                ),
                () -> assertTrue(department.isActive())
        );
    }

    @Test
    void updateDetails_shouldAllowDescriptionToBeCleared() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        department.updateDepartment("Neurology", null);

        assertAll(
                () -> assertEquals("Neurology", department.getName()),
                () -> assertNull(department.getDescription())
        );
    }

    @Test
    void updateDetails_shouldAcceptMaximumLengthsAfterTrimming() {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        String name = "A".repeat(100);
        String description = "B".repeat(500);

        department.updateDepartment(
                "  " + name + "  ",
                "  " + description + "  "
        );

        assertAll(
                () -> assertEquals(name, department.getName()),
                () -> assertEquals(description, department.getDescription())
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    void updateDetails_shouldPreserveOriginalDetailsWhenUpdateIsInvalid(
            String name,
            String description
    ) {
        Department department = Department.create(
                "Cardiology",
                "Heart consultations"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> department.updateDepartment(name, description)
        );

        assertAll(
                () -> assertEquals("Cardiology", department.getName()),
                () -> assertEquals(
                        "Heart consultations",
                        department.getDescription()
                ),
                () -> assertTrue(department.isActive())
        );
    }

    private static Stream<Arguments> invalidUpdates(){
        return Stream.of(
                Arguments.of(null, "Valid description"),
                Arguments.of("", "Valid description"),
                Arguments.of("   ", "Valid description"),
                Arguments.of("\t\n", "Valid description"),
                Arguments.of("A".repeat(101), "Valid description"),
                Arguments.of("Neurology", "B".repeat(501))
        );
    }
}
