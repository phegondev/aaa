package com.example.demo.transaction.entity;


import com.example.demo.account.entity.Account;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(length = 255)
    private String description;


    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    /**
     * Defines the many-to-one relationship with the Account entity.
     * This is the "owning" side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude // Prevents infinite recursion
    private Account account;


    // If transfer, optional source/destination tracking
    private String sourceAccount;
    private String destinationAccount;

}
