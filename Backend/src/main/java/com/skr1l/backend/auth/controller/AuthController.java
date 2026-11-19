package com.skr1l.backend.auth.controller;

import com.skr1l.backend.auth.dto.LoginRequestDto;
import com.skr1l.backend.auth.dto.LoginResponseDto;
import com.skr1l.backend.auth.service.AuthService;
import com.skr1l.backend.user.dto.UserRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(
    @Valid @RequestBody LoginRequestDto credentials) {

    return ResponseEntity.ok(new LoginResponseDto(authService.login(credentials)));
  }

  @PostMapping("/register")
  public ResponseEntity<Long> register(@Valid @RequestBody UserRequestDTO credentials) {

    Long userId = authService.register(credentials);

    return ResponseEntity.status(HttpStatus.CREATED).body(userId);
  }
}
