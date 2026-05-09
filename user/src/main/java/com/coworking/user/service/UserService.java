package com.coworking.user.service;

import com.coworking.user.dto.LoginRequest;
import com.coworking.user.dto.RegisterRequest;
import com.coworking.user.model.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    User registerUser(RegisterRequest request);
    User createUserByAdmin(RegisterRequest request);
    String login(LoginRequest request);
    List<User> getAllUsers();
    User getUserById(Long id);
    Map<Long, String> getUsernamesByIds(List<Long> ids);
    void deleteUser(Long id);
}
