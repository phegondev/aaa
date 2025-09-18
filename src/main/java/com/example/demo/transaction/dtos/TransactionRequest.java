package com.example.demo.transaction.dtos;

import com.example.demo.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)// ignor fields that are not present when returning response
@JsonIgnoreProperties(ignoreUnknown = true)// ignor fields that are not present when receiving requests in body
public class TransactionRequest {
    private TransactionType transactionType;
    private BigDecimal amount;
    private String accountNumber;
    private String destinationAccountNumber;// The receiving account number if it's a transfer
    private String description;

}
