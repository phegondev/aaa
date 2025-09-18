package com.example.demo.transaction.controller;

import com.example.demo.res.Response;
import com.example.demo.transaction.dtos.TransactionRequest;
import com.example.demo.transaction.dtos.TransactionDTO;
import com.example.demo.transaction.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Response<?>> createTransaction(
            @Valid @RequestBody TransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }


    @GetMapping("/{accountNumber}")
    public ResponseEntity<Response<List<TransactionDTO>>> getTransactionsForMyAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(transactionService.getTransactionsForMyAccount(accountNumber, page, size));
    }

}
