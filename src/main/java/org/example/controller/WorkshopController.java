package org.example.controller;

import org.example.model.Workshop;
import org.example.service.FirestoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workshop")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WorkshopController {

    private final FirestoreService firestoreService;

    public WorkshopController(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    /**
     * Fetch all workshops from the nerconWS collection.
     * GET /api/workshop/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllWorkshops() {
        try {
            List<Workshop> workshops = firestoreService.getAllWorkshops();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", workshops.size());
            response.put("data", workshops);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching workshops: " + e.getMessage());
            System.err.println("WorkshopController error: " + e.getClass().getName() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
