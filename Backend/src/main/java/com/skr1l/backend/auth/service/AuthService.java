package com.skr1l.backend.auth.service;

import com.skr1l.backend.auth.dto.LoginRequestDto;
import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;

  private final JwtEncoder jwtEncoder;

  public String login(LoginRequestDto credentials) {
    Authentication authentication = authenticationManager.authenticate(
      UsernamePasswordAuthenticationToken.unauthenticated(credentials.username(), credentials.password()));

    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("smart-warehouse")
      .subject(authentication.getName())
      .issuedAt(now)
      .expiresAt(now.plus(15, ChronoUnit.MINUTES))
      .claim("roles", authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList())
      .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public Long register(UserRequestDTO credentials) {
    return userService.createUser(credentials);
  }
}
