package com.example.demo.auth_users.services;

import com.example.demo.auth_users.dtos.LoginRequest;
import com.example.demo.auth_users.dtos.LoginResponse;
import com.example.demo.auth_users.dtos.RegistrationRequest;
import com.example.demo.auth_users.dtos.ResetPasswordRequest;
import com.example.demo.res.Response;

public interface AuthService {

    Response<String> register(RegistrationRequest request);

    Response<LoginResponse> login(LoginRequest loginRequest);

    Response<?> forgetPassword(String email);

    Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest);

}
