package com.skr1l.backend.user.service;

import com.skr1l.backend.user.User;
import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    Long createUser(UserRequestDTO userDto);

    User getUserByEmail(String email);

    User getUserByUsername(String username);

    UserResponseDTO getUserById(Long id);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO updateUser(Long id, UserRequestDTO userDto);

    void blockUser(Long id);

    void unblockUser(Long id);

    void deleteUser(Long id);
}
