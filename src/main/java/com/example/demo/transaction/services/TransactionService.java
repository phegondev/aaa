package com.example.demo.transaction.services;

import com.example.demo.res.Response;
import com.example.demo.transaction.dtos.TransactionRequest;
import com.example.demo.transaction.dtos.TransactionDTO;

import java.util.List;

public interface TransactionService {

    Response<?> createTransaction(TransactionRequest request);

    Response<List<TransactionDTO>> getTransactionsForMyAccount(String accountNumber, int page, int size);


}
