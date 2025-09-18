package com.example.demo.auth_users.dtos;

import com.example.demo.account.dtos.AccountDTO;
import com.example.demo.role.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor // ✅ Add this annotation
@AllArgsConstructor // ✅ Add this annotation
@JsonInclude(JsonInclude.Include.NON_NULL)// ignor fields that are not present when returning response
@JsonIgnoreProperties(ignoreUnknown = true)// ignor fields that are not present when receiving requests in body
public class UserDTO {

    private Long id;

    private String firstName;

    private String lastName;
    private String phoneNumber;

    private String email;

    @JsonIgnore // ✅ This annotation prevents the password field from being serialized
    private String password;
    private String profilePictureUrl;

    private boolean active;

    private List<Role> roles;

    /**
     * it prevent recursion loop
     * This annotation marks the "parent" side of the relationship.
     * Jackson will serialize the list of AccountDTOs found here.
     * It works with @JsonBackReference on the AccountDTO to avoid a circular reference.
     */
    //marks the "parent" or forward side of the relationship. When this side is serialized,
    // it will include the full details of the child objects it references.
    @JsonManagedReference
    private List<AccountDTO> accounts;

    private LocalDateTime createdAt;


}
