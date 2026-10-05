package com.skr1l.backend.user.controller;

import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.dto.UserResponseDTO;
import com.skr1l.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import java.util.List;

@Tag(name = "Users", description = "User management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "Users returned successfully")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Get user by id")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Create a user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "User already exists")
    @PostMapping
    public ResponseEntity<Long> createUser(@Valid @RequestBody UserRequestDTO userDto) {

        Long id = userService.createUser(userDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(id);
    }

    @Operation(summary = "Update a user")
    @ApiResponse(responseCode = "200", description = "User updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "User already exists")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UserRequestDTO userDto) {

        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    @Operation(summary = "Block a user")
    @ApiResponse(responseCode = "204", description = "User blocked")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/{id}/block")
    public ResponseEntity<Void> blockUser(@PathVariable Long id) {

        userService.blockUser(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unblock a user")
    @ApiResponse(responseCode = "204", description = "User unblocked")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable Long id) {

        userService.unblockUser(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
