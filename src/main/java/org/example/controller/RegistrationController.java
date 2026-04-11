package org.example.controller;

import org.example.model.RegistrationData;
import org.example.service.ExcelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registration")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RegistrationController {

    private final ExcelService excelService;

    public RegistrationController() {
        this.excelService = new ExcelService();
    }

    /**
     * Save registration data
     * POST /api/registration/save
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveRegistration(@RequestBody RegistrationData registration) {
        try {
            // Validate required fields
            if (registration.getFullname() == null || registration.getFullname().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Full name is required"));
            }

            if (registration.getEmail() == null || registration.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Email is required"));
            }

            if (registration.getDesignation() == null || registration.getDesignation().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Designation is required"));
            }

            // Save to Excel
            boolean saved = excelService.saveRegistrationToExcel(registration);

            if (saved) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Registration saved successfully");
                response.put("delegateId", registration.getDelegateId());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Failed to save registration"));
            }

        } catch (Exception e) {
            System.err.println("Error in saveRegistration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    /**
     * Get all registrations
     * GET /api/registration/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllRegistrations() {
        try {
            List<RegistrationData> registrations = excelService.getAllRegistrations();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", registrations.size());
            response.put("data", registrations);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Error fetching registrations: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /api/registration/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Registration API is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}

