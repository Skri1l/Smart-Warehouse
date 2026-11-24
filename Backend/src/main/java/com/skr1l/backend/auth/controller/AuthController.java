package com.skr1l.backend.auth.controller;

import com.skr1l.backend.auth.dto.LoginRequestDto;
import com.skr1l.backend.auth.dto.LoginResponseDto;
import com.skr1l.backend.auth.service.AuthService;
import com.skr1l.backend.user.dto.UserRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Sign-in and self-registration")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@SecurityRequirements
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Sign in and get an access token")
  @ApiResponse(responseCode = "200", description = "Signed in")
  @ApiResponse(responseCode = "400", description = "Username or password is blank")
  @ApiResponse(responseCode = "401", description = "Wrong username or password, or the user is blocked")
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(
    @Valid @RequestBody LoginRequestDto credentials) {

    return ResponseEntity.ok(new LoginResponseDto(authService.login(credentials)));
  }

  @Operation(summary = "Register a new user")
  @ApiResponse(responseCode = "201", description = "User registered")
  @ApiResponse(responseCode = "400", description = "Invalid request data")
  @ApiResponse(responseCode = "409", description = "E-mail or username is already taken")
  @PostMapping("/register")
  public ResponseEntity<Long> register(@Valid @RequestBody UserRequestDTO credentials) {

    Long userId = authService.register(credentials);

    return ResponseEntity.status(HttpStatus.CREATED).body(userId);
  }
}
