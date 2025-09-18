package com.example.demo.auth_users.repo;

import com.example.demo.auth_users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);


    long count();

}


