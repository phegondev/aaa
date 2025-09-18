package com.example.demo.auth_users.services.impl;

import com.example.demo.account.entity.Account;
import com.example.demo.account.services.AccountService;
import com.example.demo.auth_users.dtos.LoginRequest;
import com.example.demo.auth_users.dtos.LoginResponse;
import com.example.demo.auth_users.dtos.RegistrationRequest;
import com.example.demo.auth_users.dtos.ResetPasswordRequest;
import com.example.demo.auth_users.entity.PasswordResetCode;
import com.example.demo.auth_users.entity.User;
import com.example.demo.auth_users.repo.PasswordResetCodeRepo;
import com.example.demo.auth_users.repo.UserRepo;
import com.example.demo.auth_users.services.AuthService;
import com.example.demo.auth_users.services.PasswordResetCodeGenerator;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.Currency;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.notification.dtos.NotificationDTO;
import com.example.demo.notification.services.NotificationService;
import com.example.demo.res.Response;
import com.example.demo.role.entity.Role;
import com.example.demo.role.repo.RoleRepository;
import com.example.demo.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordResetCodeGenerator codeGenerator;
    private final PasswordResetCodeRepo passwordResetCodeRepo;
    private final NotificationService notificationService;
    private final AccountService accountService;


    @Value("${password.reset.link}")
    private String resetLink;


    @Override
    public Response<String> register(RegistrationRequest request) {

        List<Role> roles;
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            // Default role = CUSTOMER
            Role defaultRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("CUSTOMER ROLE not found"));
            roles = Collections.singletonList(defaultRole);
        } else {
            roles = request.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                    .collect(Collectors.toList());
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return Response.<String>builder()
                    .statusCode(400)
                    .message("Email already exists")
                    .build();
        }

        // ✅ Save new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .active(true)
                .build();
        User savedUser = userRepository.save(user);

        // ✅ Auto-create default SAVINGS account
        Account savedAccount = accountService.createAccount(AccountType.SAVINGS, savedUser);

        // ✅ Send Welcome Email
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getFirstName());

        NotificationDTO welcomeEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Welcome to Phegon Bank 🎉")
                .templateName("welcome")   // welcome.html
                .templateVariables(vars)
                .build();

        notificationService.sendEmail(welcomeEmail, user);

        // ✅ Send Account Created Email
        Map<String, Object> accountVars = new HashMap<>();
        accountVars.put("name", user.getFirstName());
        accountVars.put("accountNumber", savedAccount.getAccountNumber());
        accountVars.put("accountType", AccountType.SAVINGS.name());
        accountVars.put("currency", Currency.USD);

        NotificationDTO accountCreatedEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Your New Bank Account Has Been Created ✅")
                .templateName("account-created")   // account-created.html
                .templateVariables(accountVars)
                .build();

        notificationService.sendEmail(accountCreatedEmail, user);

        return Response.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User registered successfully. Default savings account created.")
                .data("Email of you Account details has been sent. Your Account Number is: " + savedAccount.getAccountNumber())
                .build();
    }


    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new NotFoundException("Email Not Found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Password doesn't match");
        }

        String token = tokenService.generateToken(user.getEmail());

        LoginResponse loginResponse = LoginResponse.builder()
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .token(token)
                .build();

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Login successful")
                .data(loginResponse)
                .build();
    }


    @Override
    @Transactional
    public Response<?> forgetPassword(String email) {

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User with this email not found"));

        // Delete any existing codes for this user
        passwordResetCodeRepo.deleteByUserId(user.getId());

        // Generate unique code
        String code = codeGenerator.generateUniqueCode();

        // Create and save new code
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .code(code)
                .expiryDate(calculateExpiryDate())
                .used(false)
                .build();

        passwordResetCodeRepo.save(resetCode);

        // Send email with reset link
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());
        templateVariables.put("resetLink", resetLink + code);

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Reset Code")
                .templateName("password-reset")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(notificationDTO, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password reset code sent to your email")
                .build();
    }


    @Override
    @Transactional
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest) {
        String code = resetPasswordRequest.getCode();
        String newPassword = resetPasswordRequest.getNewPassword();

        // Find and validate code
        PasswordResetCode resetCode = passwordResetCodeRepo.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Invalid reset code"));

        // Check expiration first
        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetCodeRepo.delete(resetCode); // Clean up expired code
            throw new BadRequestException("Reset code has expired");
        }

        // Update user password
        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the code immediately after successful use
        passwordResetCodeRepo.delete(resetCode);

        // Send confirmation email
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());

        NotificationDTO confirmationEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Updated Successfully")
                .templateName("password-update-confirmation")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(confirmationEmail, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password updated successfully")
                .build();
    }


    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(5); // 5 hours expiry
    }


}
