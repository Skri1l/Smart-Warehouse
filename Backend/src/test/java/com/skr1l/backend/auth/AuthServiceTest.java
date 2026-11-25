package com.skr1l.backend.auth;

import com.skr1l.backend.auth.dto.LoginRequestDto;
import com.skr1l.backend.auth.service.AuthService;
import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private UserService userService;

  @Mock
  private JwtEncoder jwtEncoder;

  @InjectMocks
  private AuthService authService;

  @Test
  void loginShouldReturnTokenWhenCredentialsAreValid() {
    var credentials = new LoginRequestDto("worker", "password");

    var authentication = UsernamePasswordAuthenticationToken.authenticated(
      "worker",
      null,
      List.of(new SimpleGrantedAuthority("ROLE_WORKER"))
    );

    Jwt jwt = mock(Jwt.class);

    when(authenticationManager.authenticate(any()))
      .thenReturn(authentication);
    when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
      .thenReturn(jwt);
    when(jwt.getTokenValue()).thenReturn("test-token");

    assertEquals("test-token", authService.login(credentials));

    ArgumentCaptor<JwtEncoderParameters> captor =
      ArgumentCaptor.forClass(JwtEncoderParameters.class);

    verify(jwtEncoder).encode(captor.capture());

    JwtClaimsSet claims = captor.getValue().getClaims();

    assertEquals("worker", claims.getSubject());
    assertEquals("smart-warehouse", claims.getClaimAsString("iss"));
    assertEquals(List.of("ROLE_WORKER"), claims.getClaimAsStringList("roles"));
    assertEquals(900, claims.getExpiresAt().getEpochSecond()
      - claims.getIssuedAt().getEpochSecond());
  }

  @Test
  void loginShouldThrowExceptionWhenCredentialsAreInvalid() {
    when(authenticationManager.authenticate(any()))
      .thenThrow(new BadCredentialsException("Invalid credentials"));

    assertThrows(BadCredentialsException.class,
      () -> authService.login(
        new LoginRequestDto("worker", "wrong-password")));

    verifyNoInteractions(jwtEncoder);
  }

  @Test
  void loginShouldThrowExceptionWhenUserIsBlocked() {
    when(authenticationManager.authenticate(any()))
      .thenThrow(new DisabledException("User is blocked"));

    assertThrows(DisabledException.class,
      () -> authService.login(
        new LoginRequestDto("worker", "password")));

    verifyNoInteractions(jwtEncoder);
  }

  @Test
  void registerShouldReturnCreatedUserId() {
    var credentials = new UserRequestDTO(
      "worker@example.com", "worker", "password");

    when(userService.createUser(credentials)).thenReturn(1L);

    assertEquals(1L, authService.register(credentials));

    verify(userService).createUser(credentials);
  }
}
