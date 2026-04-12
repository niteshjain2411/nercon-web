package org.example.controller;

import org.example.model.RegistrationData;
import org.example.service.FirestoreService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
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

    /**
     * Update registration status (approved / rejected).
     * PATCH /api/registration/{delegateId}/status
     * Body: { "status": "approved" | "rejected" }
     */
    @PatchMapping("/{delegateId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String delegateId,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || (!status.equals("approved") && !status.equals("rejected"))) {
                return ResponseEntity.badRequest().body(createErrorResponse("status must be 'approved' or 'rejected'"));
            }
            firestoreService.updateRegistrationStatus(delegateId, status);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("delegateId", delegateId);
            res.put("status", status);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating status: " + e.getMessage()));
        }
    }

    /**
     * Export selected registrations as Excel.
     * POST /api/registration/export
     * Body: { "delegateIds": ["NERCON-...", ...] }   — pass empty list to export all
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) body.getOrDefault("delegateIds", List.of());

            List<RegistrationData> all = firestoreService.getAllRegistrations();
            List<RegistrationData> rows = ids.isEmpty()
                    ? all
                    : all.stream().filter(r -> ids.contains(r.getDelegateId())).toList();

            byte[] xlsx = buildExcel(rows);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "registrations.xlsx");
            headers.setContentLength(xlsx.length);
            return new ResponseEntity<>(xlsx, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private byte[] buildExcel(List<RegistrationData> rows) throws Exception {
        String[] headers = {
            "Delegate ID", "Full Name", "Email", "Phone", "Gender",
            "Institute", "City", "State", "Designation",
            "Med Council", "Med Council Reg No.", "Attend Workshop",
            "Workshops", "Accompany Count", "Total Amount",
            "Reg Status", "Txn ID", "Txn Date", "Synopsis"
        };
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Registrations");

            // Header row style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            int rowNum = 1;
            for (RegistrationData r : rows) {
                Row row = sheet.createRow(rowNum++);
                String txnId = "", txnDate = "";
                if (r.getTxndetails() != null && !r.getTxndetails().isEmpty()) {
                    var first = r.getTxndetails().values().iterator().next();
                    txnId = first.getTxnid() != null ? first.getTxnid() : "";
                    txnDate = first.getTxndate() != null ? first.getTxndate() : "";
                }
                String[] vals = {
                    r.getDelegateId(), r.getFullname(), r.getEmail(), r.getPhone(), r.getGender(),
                    r.getInstitute(), r.getCity(), r.getState(), r.getDesignation(),
                    r.getMedcouncil(), r.getMedcouncilregnum(),
                    r.isAttendworkshop() ? "Yes" : "No",
                    r.getWorkshops() != null ? String.join(", ", r.getWorkshops()) : "",
                    String.valueOf(r.getAccompanycount()), r.getTotalAmount(),
                    r.getRegstatus(), txnId, txnDate,
                    r.getSynopsis() != null ? r.getSynopsis() : ""
                };
                for (int i = 0; i < vals.length; i++) {
                    row.createCell(i).setCellValue(vals[i] != null ? vals[i] : "");
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
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

