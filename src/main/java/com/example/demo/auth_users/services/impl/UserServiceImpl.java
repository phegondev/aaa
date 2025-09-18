package com.example.demo.auth_users.services.impl;

import com.example.demo.auth_users.dtos.UpdatePasswordRequest;
import com.example.demo.auth_users.dtos.UserDTO;
import com.example.demo.auth_users.entity.User;
import com.example.demo.auth_users.repo.UserRepo;
import com.example.demo.auth_users.services.UserService;
import com.example.demo.aws.S3Service;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.notification.dtos.NotificationDTO;
import com.example.demo.notification.services.NotificationService;
import com.example.demo.res.Response;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final S3Service s3Service;


    // Directory to save profile pictures
//    private final String uploadDir = "uploads/profile-pictures/"; //for backend
    private final String uploadDir = "/Users/mac/phegonDev/rough/abc/public/profile-picture/"; // for frontend


    @Override
    public User getCurrentLoggedInUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new NotFoundException("User is not authenticated");
        }
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User Not Found"));
    }

    @Override
    public Response<UserDTO> getMyProfile() {
        User user = getCurrentLoggedInUser();
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Users retrieved")
                .data(userDTO)
                .build();
    }

    @Override
    public Response<Page<UserDTO>> getAllUsers(int page, int size) {
        // 1. Fetch the Page of User entities from the repository
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));

        // 2. Convert the Page<User> to a Page<UserDTO>
        // The .map() method on a Page object is specifically designed for this.
        Page<UserDTO> userDTOs = users.map(user -> modelMapper.map(user, UserDTO.class));

        // 3. Return the response with the correctly typed Page<UserDTO>
        return Response.<Page<UserDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Users retrieved")
                .data(userDTOs)
                .build();
    }


    @Override
    public Response<UserDTO> updateUser(UserDTO userDto) {
        User currentUser = getCurrentLoggedInUser();

        // Update only non-null fields
        if (userDto.getFirstName() != null) currentUser.setFirstName(userDto.getFirstName());
        if (userDto.getLastName() != null) currentUser.setLastName(userDto.getLastName());
        if (userDto.getPhoneNumber() != null) currentUser.setPhoneNumber(userDto.getPhoneNumber());
        if (userDto.getRoles() != null) currentUser.setRoles(userDto.getRoles());

        userRepository.save(currentUser);


        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User updated successfully")
                .build();
    }


    @Override
    public Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        User user = getCurrentLoggedInUser();

        String newPassword = updatePasswordRequest.getNewPassword();

        // Check if the user's current password is NOT null.
        // This is the correct condition for a user with a password set.
        if (user.getPassword() != null) {
            String oldPassword = updatePasswordRequest.getOldPassword();

            // For a LOCAL user, both old and new passwords are required to change it.
            if (oldPassword == null || newPassword == null || oldPassword.isBlank() || newPassword.isBlank()) {
                throw new BadRequestException("Old and New Password Required");
            }

            // Validate the old password.
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BadRequestException("Old Password not Correct");
            }

        } else {
            // If the user's current password is null (e.g., they authenticated via OAuth),
            // we just need a new password to set one.
            if (newPassword == null || newPassword.isBlank()) {
                throw new BadRequestException("New Password Required");
            }
        }

        // Set the new password regardless of whether the old one was checked.
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Send password change confirmation email.
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());

        NotificationDTO passwordChangeEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Your Password Was Successfully Changed")
                .templateName("password-change")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(passwordChangeEmail, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password Changed Successfully")
                .build();
    }




//    public Response<?> uploadProfilePicture(MultipartFile file) {
//
//        User user = getCurrentLoggedInUser();
//
//        try {
//            // Check if the user has an existing profile picture URL.
//            // If so, delete the old file from S3 to prevent orphaned files.
//            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
//                s3Service.deleteFile(user.getProfilePictureUrl());
//            }
//
//            // Upload the new file to S3 using the dedicated S3Service
//            String s3Url = s3Service.uploadFile(file, "profile-pictures");
//
//            // Set the new URL and save the user entity to the database
//            user.setProfilePictureUrl(s3Url);
//            userRepository.save(user);
//
//            return Response.builder()
//                    .statusCode(HttpStatus.OK.value())
//                    .message("Profile picture uploaded successfully.")
//                    .data(s3Url)
//                    .build();
//
//        } catch (IOException e) {
//            return Response.builder()
//                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                    .message("Failed to upload profile picture: " + e.getMessage())
//                    .build();
//        }
//    }



    @Override
    public Response<?> uploadProfilePicture(MultipartFile file) {

        User user = getCurrentLoggedInUser();

        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Delete old picture if it exists
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                Path oldFile = Paths.get(user.getProfilePictureUrl());
                if (Files.exists(oldFile)) {
                    Files.delete(oldFile);
                }
            }

            // Generate a unique file name to avoid conflicts
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID() + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            // Save the file to the local directory
            Files.copy(file.getInputStream(), filePath);

            // Create the URL to be stored in the database
//            String fileUrl = uploadDir + newFileName; // This is a relative URL for backend
            String fileUrl = "profile-picture/" + newFileName; // This is a relative URL for frontend

            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Profile picture uploaded successfully.")
                    .data(fileUrl)
                    .build();

        } catch (IOException e) {
            // Handle file saving errors
            return Response.<UserDTO>builder()
                    .statusCode(HttpStatus.BAD_GATEWAY.value())
                    .message("Failed to upload profile picture: " + e.getMessage())
                    .build();
        }
    }


}
