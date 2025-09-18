package com.example.demo.account.services;

import com.example.demo.account.dtos.AccountDTO;
import com.example.demo.account.entity.Account;
import com.example.demo.auth_users.entity.User;
import com.example.demo.enums.AccountType;
import com.example.demo.res.Response;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {


    Account createAccount(AccountType accountType, User user);

    Response<AccountDTO> getAccountByNumber(String accountNumber);

    Response<List<AccountDTO>> getAccountsOfUser();

    Response<?> closeAccount(String accountNumber);

    Response<BigDecimal> getBalance(String accountNumber);

}
