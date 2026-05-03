package org.example.controller;

import org.example.model.Transaction;
import org.example.service.FirestoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trx")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TransactionController {

    private final FirestoreService firestoreService;

    public TransactionController(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    /**
     * Fetch all transactions from the nerconTrx collection.
     * GET /api/trx/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllTransactions() {
        try {
            List<Transaction> transactions = firestoreService.getAllTransactions();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", transactions.size());
            response.put("data", transactions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
