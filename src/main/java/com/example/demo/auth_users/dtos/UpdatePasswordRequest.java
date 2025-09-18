package com.example.demo.auth_users.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    private String oldPassword;

    @NotBlank(message = "New Password is required")
    private String newPassword;

}