package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.RegistrationData;
import org.example.service.ExcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationControllerTest {

    private MockMvc mockMvc;
    private ExcelService mockExcelService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        // Create a mock for ExcelService (constructor not called by Mockito)
        mockExcelService = mock(ExcelService.class);

        // Instantiate controller (creates real ExcelService internally, then we replace it)
        RegistrationController controller = new RegistrationController();
        Field field = RegistrationController.class.getDeclaredField("excelService");
        field.setAccessible(true);
        field.set(controller, mockExcelService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void health_returnsOkWithStatusMessage() throws Exception {
        mockMvc.perform(get("/api/registration/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Registration API is running"));
    }

    @Test
    void saveRegistration_withNullFullname_returnsBadRequest() throws Exception {
        // Arrange — fullname is null (default)
        RegistrationData data = new RegistrationData();
        data.setEmail("jane@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveRegistration_withEmptyFullname_returnsBadRequest() throws Exception {
        // Arrange
        RegistrationData data = new RegistrationData();
        data.setFullname("");
        data.setEmail("jane@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveRegistration_withNullEmail_returnsBadRequest() throws Exception {
        // Arrange — email is null (default)
        RegistrationData data = new RegistrationData();
        data.setFullname("Jane Doe");

        // Act & Assert
        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveRegistration_withEmptyEmail_returnsBadRequest() throws Exception {
        // Arrange
        RegistrationData data = new RegistrationData();
        data.setFullname("Jane Doe");
        data.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveRegistration_withNullDesignation_returnsBadRequest() throws Exception {
        RegistrationData data = new RegistrationData();
        data.setFullname("Jane Doe");
        data.setEmail("jane@example.com");

        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Designation is required"));
    }

    @Test
    void saveRegistration_withEmptyDesignation_returnsBadRequest() throws Exception {
        RegistrationData data = new RegistrationData();
        data.setFullname("Jane Doe");
        data.setEmail("jane@example.com");
        data.setDesignation("");

        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Designation is required"));
    }

    @Test
    void saveRegistration_whenServiceReturnsTrue_returnsOk() throws Exception {
        // Arrange
        when(mockExcelService.saveRegistrationToExcel(any())).thenReturn(true);
        RegistrationData data = buildSampleRegistration();

        // Act & Assert
        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration saved successfully"));
    }

    @Test
    void saveRegistration_whenServiceReturnsFalse_returnsInternalServerError() throws Exception {
        // Arrange
        when(mockExcelService.saveRegistrationToExcel(any())).thenReturn(false);
        RegistrationData data = buildSampleRegistration();

        // Act & Assert
        mockMvc.perform(post("/api/registration/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getAllRegistrations_returnsSuccessWithCountAndData() throws Exception {
        // Arrange
        when(mockExcelService.getAllRegistrations())
                .thenReturn(Collections.singletonList(buildSampleRegistration()));

        // Act & Assert
        mockMvc.perform(get("/api/registration/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void getAllRegistrations_whenEmpty_returnsCountZero() throws Exception {
        // Arrange
        when(mockExcelService.getAllRegistrations()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/registration/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(0));
    }

    // -- helpers --

    private RegistrationData buildSampleRegistration() {
        RegistrationData data = new RegistrationData();
        data.setFullname("Jane Doe");
        data.setEmail("jane@example.com");
        data.setPhone("9876543210");
        data.setGender("Female");
        data.setInstitute("AIIMS");
        data.setCity("Delhi");
        data.setState("Delhi");
        data.setDesignation("Professor");
        data.setDelegateId("DEL001");
        return data;
    }
}
