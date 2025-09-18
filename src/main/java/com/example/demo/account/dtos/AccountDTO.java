package com.example.demo.account.dtos;

import com.example.demo.auth_users.dtos.UserDTO;
import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.Currency;
import com.example.demo.transaction.dtos.TransactionDTO;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

    private Long id;

    private String accountNumber;

    private BigDecimal balance;

    private AccountType accountType;

    private Currency currency;

    private AccountStatus status;
    /**
     * This annotation marks the "child" side of the relationship.
     * When Jackson is serializing an AccountDTO, it will NOT serialize the
     * UserDTO here, effectively breaking the recursion loop.
     */
    //marks the "child" or backward side of the relationship.
    // When this side is serialized, it will not include the parent object it references,
    // effectively breaking the serialization loop.
    @JsonBackReference
     private UserDTO user;

    private LocalDateTime createdAt;

    //marks the "parent" or forward side of the relationship. When this side is serialized,
    // it will include the full details of the child objects it references.
    @JsonManagedReference
    private List<TransactionDTO> transactions;

}
