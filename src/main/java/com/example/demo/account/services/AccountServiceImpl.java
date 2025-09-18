package com.example.demo.account.services;

import com.example.demo.account.dtos.AccountDTO;
import com.example.demo.account.entity.Account;
import com.example.demo.account.repo.AccountRepo;
import com.example.demo.auth_users.entity.User;
import com.example.demo.auth_users.services.UserService;
import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.Currency;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.res.Response;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    public Account createAccount(AccountType accountType, User user) {


        // Generate unique account number
        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(accountType)
                .currency(Currency.USD)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);


    }

    @Override
    public Response<AccountDTO> getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return Response.<AccountDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account found")
                .data(modelMapper.map(account, AccountDTO.class))
                .build();
    }

    @Override
    public Response<List<AccountDTO>> getAccountsOfUser() {
        User user = userService.getCurrentLoggedInUser();

        List<AccountDTO> accounts = accountRepository.findByUserId(user.getId())
                .stream()
                .map(account -> modelMapper.map(account, AccountDTO.class))
                .collect(Collectors.toList());

        return Response.<List<AccountDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User accounts fetched successfully")
                .data(accounts)
                .build();
    }

    @Override
    public Response<?> closeAccount(String accountNumber) {
        User user = userService.getCurrentLoggedInUser();

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!user.getAccounts().contains(account)) {
            throw new NotFoundException("Account doesn't belong to you");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Account balance must be zero before closing");
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account closed successfully")
                .build();
    }

    @Override
    public Response<BigDecimal> getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return Response.<BigDecimal>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Balance fetched successfully")
                .data(account.getBalance())
                .build();
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate a random 10-digit number, ensuring it doesn't start with 0
            accountNumber = String.valueOf(1_000_000_000L +
                    (long) (Math.random() * 9_000_000_000L));
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

}
