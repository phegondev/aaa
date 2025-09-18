package com.example.demo.audit_dashboard.controller;

import com.example.demo.auth_users.dtos.UserDTO;
import com.example.demo.account.dtos.AccountDTO;
import com.example.demo.audit_dashboard.service.AuditorService;
import com.example.demo.transaction.dtos.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditorController {

    private final AuditorService adminService;

    @GetMapping("/totals")
    public ResponseEntity<Map<String, Long>> getSystemTotals() {
        Map<String, Long> totals = adminService.getSystemTotals();
        return ResponseEntity.ok(totals);
    }

    @GetMapping("/users")
    public ResponseEntity<UserDTO> findUserByEmail(@RequestParam String email) {
        Optional<UserDTO> user = adminService.findUserByEmail(email);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/accounts")
    public ResponseEntity<AccountDTO> findAccountDetailsByAccountNumber(@RequestParam String accountNumber) {
        Optional<AccountDTO> account = adminService.findAccountDetailsByAccountNumber(accountNumber);
        return account.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/transactions/by-account")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByAccountNumber(@RequestParam String accountNumber) {
        List<TransactionDTO> transactions = adminService.findTransactionsByAccountNumber(accountNumber);
        if (transactions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/by-id")
    public ResponseEntity<TransactionDTO> getTransactionById(@RequestParam Long id) {
        Optional<TransactionDTO> transaction = adminService.findTransactionById(id);
        return transaction.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}