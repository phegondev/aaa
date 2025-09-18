package com.example.demo.transaction.services;

import com.example.demo.account.entity.Account;
import com.example.demo.account.repo.AccountRepo;
import com.example.demo.auth_users.entity.User;
import com.example.demo.auth_users.services.UserService;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.InsufficientBalanceException;
import com.example.demo.exceptions.InvalidTransactionException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.notification.dtos.NotificationDTO;
import com.example.demo.notification.services.NotificationService;
import com.example.demo.res.Response;
import com.example.demo.transaction.dtos.TransactionDTO;
import com.example.demo.transaction.dtos.TransactionRequest;
import com.example.demo.transaction.entity.Transaction;
import com.example.demo.transaction.repo.TransactionRepo;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepo accountRepository;
    private final TransactionRepo transactionRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public Response<?> createTransaction(TransactionRequest request) {

        Transaction transaction = new Transaction();
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        switch (request.getTransactionType()) {
            case DEPOSIT -> handleDeposit(request, transaction);
            case WITHDRAWAL -> handleWithdrawal(request, transaction);
            case TRANSFER -> handleTransfer(request, transaction);
            default -> throw new InvalidTransactionException("Invalid transaction type");
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction savedTxn = transactionRepository.save(transaction);

        // ✅ Send notifications
        sendTransactionNotifications(savedTxn);

        return Response.builder()
                .statusCode(200)
                .message("Transaction successful")
                .build();
    }


    @Override
    public Response<List<TransactionDTO>> getTransactionsForMyAccount(String accountNumber, int page, int size) {

        // Get the currently logged-in user
        User user = userService.getCurrentLoggedInUser();

        // Find the account by its number
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        // ✅ Add the security check here
        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Account does not belong to the authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepository.findByAccount_AccountNumber(accountNumber, pageable);

        // Convert Page<Transaction> content to List<TransactionDTO>

        List<TransactionDTO> transactionDTOS = txns.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO.class))
                .toList();

        return Response.<List<TransactionDTO>>builder()
                .statusCode(200)
                .message("Transactions retrieved")
                .data(transactionDTOS)
                .meta(Map.of(
                        "currentPage", txns.getNumber(),
                        "totalItems", txns.getTotalElements(),
                        "totalPages", txns.getTotalPages(),
                        "pageSize", txns.getSize()
                )).build();
    }


    /**
     * Helper method to notify sender/receiver
     */
    private void sendTransactionNotifications(Transaction txn) {
        User user = txn.getAccount().getUser();
        String subject;
        String template;
        Map<String, Object> vars = new HashMap<>();

        vars.put("name", user.getFirstName());
        vars.put("amount", txn.getAmount());
        vars.put("accountNumber", txn.getAccount().getAccountNumber());
        vars.put("date", txn.getTransactionDate());
        vars.put("balance", txn.getAccount().getBalance());

        if (txn.getTransactionType() == TransactionType.DEPOSIT) {
            subject = "Credit Alert";
            template = "credit-alert";
            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(vars)
                    .build(), user);

        } else if (txn.getTransactionType() == TransactionType.WITHDRAWAL) {
            subject = "Debit Alert";
            template = "debit-alert";
            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(vars)
                    .build(), user);

        } else if (txn.getTransactionType() == TransactionType.TRANSFER) {
            // Sender DEBIT alert
            subject = "Debit Alert";
            template = "debit-alert";
            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(vars)
                    .build(), user);

            // Receiver CREDIT alert
            Account destination = accountRepository.findByAccountNumber(txn.getDestinationAccount())
                    .orElseThrow(() -> new NotFoundException("Destination account not found"));

            User receiver = destination.getUser();

            Map<String, Object> recvVars = new HashMap<>();
            recvVars.put("name", receiver.getFirstName());
            recvVars.put("amount", txn.getAmount());
            recvVars.put("accountNumber", destination.getAccountNumber());
            recvVars.put("date", txn.getTransactionDate());
            recvVars.put("balance", destination.getBalance());

            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(receiver.getEmail())
                    .subject("Credit Alert")
                    .templateName("credit-alert")
                    .templateVariables(recvVars)
                    .build(), receiver);
        }
    }


    private void handleDeposit(TransactionRequest request, Transaction transaction) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        account.setBalance(account.getBalance().add(request.getAmount()));
        transaction.setAccount(account);
        accountRepository.save(account);
    }


    private void handleWithdrawal(TransactionRequest request, Transaction transaction) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        transaction.setAccount(account);
        accountRepository.save(account);
    }


    private void handleTransfer(TransactionRequest request, Transaction transaction) {

        Account sourceAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Source account not found"));

        Account destination = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new NotFoundException("Destination account not found"));

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account");
        }

        // Deduct from source
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(sourceAccount);

        // Add to destination
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        accountRepository.save(destination);

        transaction.setAccount(sourceAccount);
        transaction.setSourceAccount(sourceAccount.getAccountNumber());
        transaction.setDestinationAccount(destination.getAccountNumber());

    }

}
