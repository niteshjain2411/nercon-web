package org.example.controller;

import org.example.model.RegistrationData;
import org.example.service.FirestoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/registration")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RegistrationController {

    private final FirestoreService firestoreService;

    public RegistrationController(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    /**
     * Save registration data to Firestore.
     * POST /api/registration/save
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveRegistration(@RequestBody RegistrationData registration) {
        try {
            if (registration.getFullname() == null || registration.getFullname().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Full name is required"));
            }
            if (registration.getEmail() == null || registration.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Email is required"));
            }
            if (registration.getDesignation() == null || registration.getDesignation().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Designation is required"));
            }

            // Auto-generate delegateId if not supplied by the client
            if (registration.getDelegateId() == null || registration.getDelegateId().isBlank()) {
                registration.setDelegateId("NERCON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            // Mark registration as successful before persisting
            registration.setRegstatus("success");

            boolean saved = firestoreService.saveRegistration(registration);

            if (saved) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Registration saved successfully");
                response.put("delegateId", registration.getDelegateId());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to save registration"));
            }
        } catch (Exception e) {
            System.err.println("Error in saveRegistration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    /**
     * Upload payment screenshot to Firebase Storage.
     * POST /api/registration/upload/payment
     */
    @PostMapping(value = "/upload/payment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPaymentImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("delegateId") String delegateId) {
        return handleImageUpload(file, delegateId, "payment");
    }

    /**
     * Upload PG bonafide letter to Firebase Storage.
     * POST /api/registration/upload/bonafide
     */
    @PostMapping(value = "/upload/bonafide", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBonafideImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("delegateId") String delegateId) {
        return handleImageUpload(file, delegateId, "bonafide");
    }

    /**
     * Get all registrations from Firestore.
     * GET /api/registration/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllRegistrations() {
        try {
            List<RegistrationData> registrations = firestoreService.getAllRegistrations();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", registrations.size());
            response.put("data", registrations);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching registrations: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint.
     * GET /api/registration/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Registration API is running");
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------

    private ResponseEntity<?> handleImageUpload(MultipartFile file, String delegateId, String imageType) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No file provided"));
            }
            if (delegateId == null || delegateId.isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("delegateId is required"));
            }
            String url = firestoreService.uploadImage(file, delegateId, imageType);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("url", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error uploading " + imageType + " image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload image: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}

