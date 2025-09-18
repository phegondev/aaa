package com.example.demo.auth_users.services;

import com.example.demo.auth_users.dtos.UpdatePasswordRequest;
import com.example.demo.auth_users.dtos.UserDTO;
import com.example.demo.auth_users.entity.User;
import com.example.demo.res.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {


    User getCurrentLoggedInUser();
    Response<UserDTO> getMyProfile();

    Response<Page<UserDTO>> getAllUsers(int page, int size);

    Response<UserDTO> updateUser(UserDTO userDto);

    Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest);

    Response<?> uploadProfilePicture(MultipartFile file);
}
