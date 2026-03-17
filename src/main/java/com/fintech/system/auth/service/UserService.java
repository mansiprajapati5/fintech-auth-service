package com.fintech.system.auth.service;


import com.fintech.system.auth.Model.User;
import com.fintech.system.auth.Decorator.AuthResponse;
import com.fintech.system.auth.Decorator.LoginRequest;
import com.fintech.system.auth.Decorator.RegisterRequest;
import com.fintech.system.auth.Decorator.UserUpdateRequest;
import jakarta.mail.MessagingException;
import org.springframework.data.domain.Page;

public interface UserService {
    AuthResponse createUser(RegisterRequest request) throws MessagingException;

    AuthResponse login(LoginRequest request);

    User getUserById(String id);

    Page<User> getAllUsers(int page, int size);

    AuthResponse updateUser(String id, UserUpdateRequest request);

    void deleteUser(String id);
}
