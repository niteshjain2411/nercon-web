package org.example.service;

import org.example.model.RegistrationData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelServiceTest {

    // Matches ExcelService.EXCEL_FILE_PATH
    private static final String EXCEL_FILE_PATH = "registrations/NERCON_2026_Registrations.xlsx";

    @AfterEach
    void cleanup() {
        new File(EXCEL_FILE_PATH).delete();
    }

    @Test
    void getAllRegistrations_whenNoFileExists_returnsEmptyList() {
        // Arrange
        new File(EXCEL_FILE_PATH).delete();
        ExcelService service = new ExcelService();

        // Act
        List<RegistrationData> result = service.getAllRegistrations();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void saveRegistrationToExcel_withValidData_returnsTrue() {
        // Arrange
        ExcelService service = new ExcelService();
        RegistrationData data = buildSampleRegistration("DEL001");

        // Act
        boolean result = service.saveRegistrationToExcel(data);

        // Assert
        assertTrue(result);
        assertTrue(new File(EXCEL_FILE_PATH).exists());
    }

    @Test
    void saveRegistrationToExcel_appendsToExistingFile_returnsTwoRecords() {
        // Arrange
        ExcelService service = new ExcelService();
        RegistrationData first = buildSampleRegistration("DEL001");
        RegistrationData second = buildSampleRegistration("DEL002");

        // Act
        service.saveRegistrationToExcel(first);
        service.saveRegistrationToExcel(second);
        List<RegistrationData> all = service.getAllRegistrations();

        // Assert
        assertEquals(2, all.size());
    }

    @Test
    void getAllRegistrations_afterSave_returnsCorrectDelegateIdAndName() {
        // Arrange
        ExcelService service = new ExcelService();
        RegistrationData data = buildSampleRegistration("DEL001");

        // Act
        service.saveRegistrationToExcel(data);
        List<RegistrationData> all = service.getAllRegistrations();

        // Assert
        assertEquals(1, all.size());
        assertEquals("DEL001", all.get(0).getDelegateId());
        assertEquals("Jane Doe", all.get(0).getFullname());
        assertEquals("jane@example.com", all.get(0).getEmail());
        assertEquals("Professor", all.get(0).getDesignation());
    }

    @Test
    void saveRegistrationToExcel_withNullWorkshops_returnsTrue() {
        // Arrange
        ExcelService service = new ExcelService();
        RegistrationData data = buildSampleRegistration("DEL003");
        data.setWorkshops(null); // edge case: null workshops list

        // Act
        boolean result = service.saveRegistrationToExcel(data);

        // Assert — no NPE, returns true
        assertTrue(result);
    }

    // -- helpers --

    private RegistrationData buildSampleRegistration(String delegateId) {
        return new RegistrationData(
                "Jane Doe", "jane@example.com", "9876543210", "Female",
            "AIIMS", "Delhi", "Delhi", "MC123", "REG456", "Professor",
                Arrays.asList("Workshop A"), "1", "TXN001", "2026-04-09", "5000",
                delegateId);
    }
}
