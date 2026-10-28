package com.nathan.hospitalmanagementsystem.department.controller;

import com.nathan.hospitalmanagementsystem.common.exception.GlobalExceptionHandler;
import com.nathan.hospitalmanagementsystem.controller.DepartmentController;
import com.nathan.hospitalmanagementsystem.department.dto.CreateDepartmentRequest;
import com.nathan.hospitalmanagementsystem.department.dto.DepartmentResponse;
import com.nathan.hospitalmanagementsystem.department.exception.DepartmentAlreadyExistsException;
import com.nathan.hospitalmanagementsystem.department.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import(GlobalExceptionHandler.class)
public class DepartmentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private DepartmentService service;

    private static final LocalDateTime localDateTime = LocalDateTime.now();


    @Test
    void createDepartment_showReturnCreatedResponseAndNormalizeInput() throws Exception {
        CreateDepartmentRequest createDepartmentRequest = new CreateDepartmentRequest("   Neurology   ", "    Brain-related consultation    ");

        DepartmentResponse response = new DepartmentResponse(12L,"Neurology","Brain-related consultation",true,localDateTime);

        when(service.createDepartment(createDepartmentRequest)).thenReturn(response);

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepartmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/v1/departments/12"
                ))
                .andExpect(jsonPath("$.id").value(12L))
                .andExpect(jsonPath("$.name").value("Neurology"))
                .andExpect(jsonPath("$.description").value("Brain-related consultation"))
                .andExpect(jsonPath("$.active").value(true));

        assertEquals(localDateTime,response.createdAt());

        verify(service).createDepartment(createDepartmentRequest);
    }

    @Test
    void createDepartment_shouldAllowToPassWithoutDescription() throws Exception {
        CreateDepartmentRequest createDepartmentRequest = new CreateDepartmentRequest("   Neurology   ", null);

        DepartmentResponse response = new DepartmentResponse(12L,"Neurology",null,true,localDateTime);

        when(service.createDepartment(createDepartmentRequest)).thenReturn(response);

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDepartmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/v1/departments/12"
                ))
                .andExpect(jsonPath("$.id").value(12L))
                .andExpect(jsonPath("$.name").value("Neurology"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).createDepartment(createDepartmentRequest);

    }


    @Test
    void createDepartment_shouldAcceptMaximumLengthTrimming() throws Exception {
        String name = "A".repeat(100);
        String description = "S".repeat(500);

        CreateDepartmentRequest request = new CreateDepartmentRequest(name,description);

        DepartmentResponse response = new DepartmentResponse(12L,name,description,true,localDateTime);

        when(service.createDepartment(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(service).createDepartment(request);
    }

    @Test
    void createDepartment_shouldReturnConflictForDuplicateName() throws Exception {
        CreateDepartmentRequest createDepartmentRequest = new CreateDepartmentRequest("   Neurology   ", null);

        when(service.createDepartment(createDepartmentRequest)).thenThrow(new DepartmentAlreadyExistsException("Neurology"));

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepartmentRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A department named 'Neurology' already exists."))
                .andExpect(jsonPath("$.path").value("/api/v1/departments"));

    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void createDepartment_shouldRejectInvalidFields(String body, String field,String message) throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/departments"))
                .andExpect(jsonPath("$.errors['" + field +"']").value(message));

        verifyNoInteractions(service);

    }


    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":", "null",""})
    void createDepartment_shouldRejectUnreadableOrMissingBody(String body) throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoInteractions(service);
    }


    private static Stream<Arguments> invalidRequests(){
        String requiredName = "Department Name cannot be empty";

        return Stream.of(
                Arguments.of(
                        "{}",
                        "name",
                        requiredName
                ),
                Arguments.of(
                        "{\"name\":null}",
                        "name",
                        requiredName
                ),
                Arguments.of(
                        "{\"name\":null}",
                        "name",
                        requiredName
                ),
                Arguments.of(
                        "{\"name\":\"\"}",
                        "name",
                        requiredName
                ),
                Arguments.of(
                        "{\"name\":\"   \"}",
                        "name",
                        requiredName
                ),
                Arguments.of(
                        "{\"name\":\"\\t\\n\"}",
                        "name",
                        requiredName
                ),
                Arguments.of(
                        "{\"name\":\"" + "A".repeat(101) + "\"}",
                        "name",
                        "Department name cannot exceed 100 characters"
                ),
                Arguments.of(
                        "{\"name\":\"Cardiology\",\"description\":\""
                                + "B".repeat(501) + "\"}",
                        "description",
                        "Description cannot exceed 500 characters"
                )
        );
    }

}
