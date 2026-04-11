package org.example.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationDataTest {

    @Test
    void defaultConstructor_createsInstanceWithNullFields() {
        RegistrationData data = new RegistrationData();

        assertNull(data.getFullname());
        assertNull(data.getEmail());
        assertNull(data.getPhone());
        assertNull(data.getDelegateId());
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        // Arrange
        List<String> workshops = Arrays.asList("WS1", "WS2");

        // Act
        RegistrationData data = new RegistrationData(
                "Jane Doe", "jane@example.com", "9876543210", "Female",
            "AIIMS", "Delhi", "Delhi", "MC123", "REG456", "Professor",
                workshops, "2", "TXN789", "2026-04-09", "5000", "DEL001");

        // Assert
        assertEquals("Jane Doe", data.getFullname());
        assertEquals("jane@example.com", data.getEmail());
        assertEquals("9876543210", data.getPhone());
        assertEquals("Female", data.getGender());
        assertEquals("AIIMS", data.getInstitute());
        assertEquals("Delhi", data.getCity());
        assertEquals("Delhi", data.getState());
        assertEquals("MC123", data.getMedcouncil());
        assertEquals("REG456", data.getRegistration());
        assertEquals("Professor", data.getDesignation());
        assertEquals(workshops, data.getWorkshops());
        assertEquals("2", data.getAccompany());
        assertEquals("TXN789", data.getTxnid());
        assertEquals("2026-04-09", data.getTxndate());
        assertEquals("5000", data.getTotalAmount());
        assertEquals("DEL001", data.getDelegateId());
    }

    @Test
    void setFullname_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setFullname("John Doe");
        assertEquals("John Doe", data.getFullname());
    }

    @Test
    void setEmail_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setEmail("john@example.com");
        assertEquals("john@example.com", data.getEmail());
    }

    @Test
    void setPhone_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setPhone("9999999999");
        assertEquals("9999999999", data.getPhone());
    }

    @Test
    void setGender_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setGender("Male");
        assertEquals("Male", data.getGender());
    }

    @Test
    void setInstitute_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setInstitute("AIIMS");
        assertEquals("AIIMS", data.getInstitute());
    }

    @Test
    void setCity_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setCity("Mumbai");
        assertEquals("Mumbai", data.getCity());
    }

    @Test
    void setState_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setState("Maharashtra");
        assertEquals("Maharashtra", data.getState());
    }

    @Test
    void setMedcouncil_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setMedcouncil("MC001");
        assertEquals("MC001", data.getMedcouncil());
    }

    @Test
    void setRegistration_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setRegistration("REG001");
        assertEquals("REG001", data.getRegistration());
    }

    @Test
    void setDesignation_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setDesignation("Consultant");
        assertEquals("Consultant", data.getDesignation());
    }

    @Test
    void setWorkshops_updatesValue() {
        RegistrationData data = new RegistrationData();
        List<String> workshops = Arrays.asList("WS1", "WS2", "WS3");
        data.setWorkshops(workshops);
        assertEquals(workshops, data.getWorkshops());
    }

    @Test
    void setAccompany_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setAccompany("1");
        assertEquals("1", data.getAccompany());
    }

    @Test
    void setTxnid_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setTxnid("TXN123");
        assertEquals("TXN123", data.getTxnid());
    }

    @Test
    void setTxndate_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setTxndate("2026-04-09");
        assertEquals("2026-04-09", data.getTxndate());
    }

    @Test
    void setTotalAmount_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setTotalAmount("7500");
        assertEquals("7500", data.getTotalAmount());
    }

    @Test
    void setDelegateId_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setDelegateId("DEL999");
        assertEquals("DEL999", data.getDelegateId());
    }
}
