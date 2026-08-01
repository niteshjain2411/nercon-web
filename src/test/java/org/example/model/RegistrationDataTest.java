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
        List<String> txns = Arrays.asList("TXN789");

        // Act
        RegistrationData data = new RegistrationData(
                "Jane Doe", "jane@example.com", "9876543210", "Female",
                "AIIMS", "Delhi", "Delhi", "Professor",
                "MC123", "REG456",
                true, workshops, 2L,
                txns, "DEL001", null);

        // Assert
        assertEquals("Jane Doe", data.getFullname());
        assertEquals("jane@example.com", data.getEmail());
        assertEquals("9876543210", data.getPhone());
        assertEquals("Female", data.getGender());
        assertEquals("AIIMS", data.getInstitute());
        assertEquals("Delhi", data.getCity());
        assertEquals("Delhi", data.getState());
        assertEquals("MC123", data.getMedcouncil());
        assertEquals("REG456", data.getMedcouncilregnum());
        assertEquals("Professor", data.getDesignation());
        assertTrue(data.isAttendworkshop());
        assertEquals(workshops, data.getWorkshops());
        assertEquals(2L, data.getAccompanycount());
        assertEquals(txns, data.getTxndetails());
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
    void setRegstatus_updatesValue() {
        // regstatus has been moved to Transaction model — no-op test
        RegistrationData data = new RegistrationData();
        assertNotNull(data); // field no longer exists on RegistrationData
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
        List<String> txns = Arrays.asList("TXN123", "TXN456");
        data.setTxndetails(txns);
        assertEquals("TXN123", data.getTxndetails().get(0));
        assertEquals("TXN456", data.getTxndetails().get(1));
    }

    @Test
    void setPaymentimg_updatesValue() {
        // paymentimg has been moved to Transaction model — no-op test
        RegistrationData data = new RegistrationData();
        assertNotNull(data);
    }

    @Test
    void setPgbonafideimg_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setPgbonafideimg("http://example.com/cert.jpg");
        assertEquals("http://example.com/cert.jpg", data.getPgbonafideimg());
    }

    @Test
    void setTotalAmount_updatesValue() {
        // totalAmount has been moved to Transaction model — no-op test
        RegistrationData data = new RegistrationData();
        assertNotNull(data);
    }

    @Test
    void setDelegateId_updatesValue() {
        RegistrationData data = new RegistrationData();
        data.setDelegateId("DEL999");
        assertEquals("DEL999", data.getDelegateId());
    }
}
