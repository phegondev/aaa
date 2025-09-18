package com.example.demo.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)// ignors fields that are not present in body when receiving requests
public class ResetPasswordRequest {

    //will be used to request for forgot password
    private String email;
    //will be used to set new password
    private String code;
    private String newPassword;
}