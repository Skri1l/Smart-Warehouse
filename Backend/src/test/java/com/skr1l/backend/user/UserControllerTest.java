package com.skr1l.backend.user;

import com.skr1l.backend.user.controller.UserController;
import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.dto.UserResponseDTO;
import com.skr1l.backend.user.enums.UserRole;
import com.skr1l.backend.user.enums.UserStatus;
import com.skr1l.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getAllUsersShouldReturnUsers() throws Exception {

        UserResponseDTO user = new UserResponseDTO(
                1L,
                "test@gmail.com",
                "testUser",
                UserRole.WORKER,
                UserStatus.ACTIVE
        );

        when(userService.getAllUsers())
                .thenReturn(List.of(user));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].email").value("test@gmail.com"))
                .andExpect(jsonPath("$[0].username").value("testUser"));

        verify(userService).getAllUsers();
    }

    @Test
    void getUserByIdShouldReturnUser() throws Exception {

        UserResponseDTO user = new UserResponseDTO(
                1L,
                "test@gmail.com",
                "testUser",
                UserRole.WORKER,
                UserStatus.ACTIVE
        );

        when(userService.getUserById(1L))
                .thenReturn(user);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.username").value("testUser"));

        verify(userService).getUserById(1L);
    }

    @Test
    void createUserShouldReturnCreated() throws Exception {

        when(userService.createUser(any(UserRequestDTO.class)))
                .thenReturn(1L);

        String json = """
                {
                  "email": "test@gmail.com",
                  "username": "testUser",
                  "password": "testPassword"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("1"));

        verify(userService).createUser(any(UserRequestDTO.class));
    }

    @Test
    void updateUserShouldReturnUpdatedUser() throws Exception {

        UserResponseDTO response = new UserResponseDTO(
                1L,
                "new@gmail.com",
                "newUser",
                UserRole.WORKER,
                UserStatus.ACTIVE
        );

        when(userService.updateUser(
                eq(1L),
                any(UserRequestDTO.class)
        )).thenReturn(response);

        String json = """
                {
                  "email": "new@gmail.com",
                  "username": "newUser",
                  "password": "newPassword"
                }
                """;

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("new@gmail.com"))
                .andExpect(jsonPath("$.username").value("newUser"));

        verify(userService).updateUser(
                eq(1L),
                any(UserRequestDTO.class)
        );
    }

    @Test
    void blockUserShouldReturnNoContent() throws Exception {

        mockMvc.perform(patch("/users/1/block"))
                .andExpect(status().isNoContent());

        verify(userService).blockUser(1L);
    }

    @Test
    void unblockUserShouldReturnNoContent() throws Exception {

        mockMvc.perform(patch("/users/1/unblock"))
                .andExpect(status().isNoContent());

        verify(userService).unblockUser(1L);
    }

    @Test
    void deleteUserShouldReturnNoContent() throws Exception {

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }
}