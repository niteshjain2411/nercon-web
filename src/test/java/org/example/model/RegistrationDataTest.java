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
        java.util.Map<String, org.example.model.Transaction> txns = new java.util.LinkedHashMap<>();
        txns.put("t1", new org.example.model.Transaction("TXN789", "2026-04-09"));

        // Act
        RegistrationData data = new RegistrationData(
                "Jane Doe", "jane@example.com", "9876543210", "Female",
                "AIIMS", "Delhi", "Delhi", "Professor",
                "MC123", "Active", "REG456",
                true, workshops, 2L,
                txns, "5000", "DEL001", "http://pay.img", null);

        // Assert
        assertEquals("Jane Doe", data.getFullname());
        assertEquals("jane@example.com", data.getEmail());
        assertEquals("9876543210", data.getPhone());
        assertEquals("Female", data.getGender());
        assertEquals("AIIMS", data.getInstitute());
        assertEquals("Delhi", data.getCity());
        assertEquals("Delhi", data.getState());
        assertEquals("MC123", data.getMedcouncil());
        assertEquals("Active", data.getRegstatus());
        assertEquals("REG456", data.getMedcouncilregnum());
        assertEquals("Professor", data.getDesignation());
        assertTrue(data.isAttendworkshop());
        assertEquals(workshops, data.getWorkshops());
        assertEquals(2L, data.getAccompanycount());
        assertEquals(txns, data.getTxndetails());
        assertEquals("5000", data.getTotalAmount());
        assertEquals("DEL001", data.getDelegateId());
        assertEquals("http://pay.img", data.getPaymentimg());
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
    void setRegstatus_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setRegstatus("Active");
        assertEquals("Active", data.getRegstatus());
    }

    @Test
    void setMedcouncilregnum_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setMedcouncilregnum("REG001");
        assertEquals("REG001", data.getMedcouncilregnum());
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
    void setAccompanycount_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setAccompanycount(3L);
        assertEquals(3L, data.getAccompanycount());
    }

    @Test
    void setAttendworkshop_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setAttendworkshop(true);
        assertTrue(data.isAttendworkshop());
    }

    @Test
    void setTxndetails_updatesValue() {
        RegistrationData data = new RegistrationData();
        java.util.Map<String, org.example.model.Transaction> txns = new java.util.LinkedHashMap<>();
        txns.put("t1", new org.example.model.Transaction("TXN123", "2026-04-09"));
        data.setTxndetails(txns);
        assertEquals("TXN123", data.getTxndetails().get("t1").getTxnid());
        assertEquals("2026-04-09", data.getTxndetails().get("t1").getTxndate());
    }

    @Test
    void setPaymentimg_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setPaymentimg("http://example.com/pay.jpg");
        assertEquals("http://example.com/pay.jpg", data.getPaymentimg());
    }

    @Test
    void setPgbonafideimg_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setPgbonafideimg("http://example.com/cert.jpg");
        assertEquals("http://example.com/cert.jpg", data.getPgbonafideimg());
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
