package com.skr1l.backend.auth;

import com.skr1l.backend.user.enums.UserRole;
import com.skr1l.backend.user.enums.UserStatus;
import com.skr1l.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
  "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

  private static final String USERNAME = "auth-test-worker";
  private static final String PASSWORD = "test-password";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                    "email": "auth-test@example.com",
                                    "username": "auth-test-worker",
                                    "password": "test-password"
                                }
                                """))
      .andExpect(status().isCreated());
  }

  @Test
  void registerShouldPersistUserWithEncodedPassword() {
    var user = userRepository.findByUsername(USERNAME)
      .orElseThrow();

    assertEquals(UserRole.WORKER, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertNotEquals(PASSWORD, user.getPasswordHash());
    assertTrue(passwordEncoder.matches(
      PASSWORD, user.getPasswordHash()));
  }

  @Test
  void registerShouldRejectDuplicateUser() throws Exception {
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                    "email": "auth-test@example.com",
                                    "username": "auth-test-worker",
                                    "password": "test-password"
                                }
                                """))
      .andExpect(status().isConflict());
  }

  @Test
  void registerShouldRejectInvalidData() throws Exception {
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                    "email": "",
                                    "username": "",
                                    "password": ""
                                }
                                """))
      .andExpect(status().isBadRequest());
  }

  @Test
  void loginShouldReturnValidJwt() throws Exception {
    String token = login();

    var jwt = jwtDecoder.decode(token);

    assertEquals(USERNAME, jwt.getSubject());
    assertTrue(jwt.getClaimAsStringList("roles")
      .contains("ROLE_WORKER"));
  }

  @Test
  void loginShouldRejectInvalidPassword() throws Exception {
    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                    "username": "auth-test-worker",
                                    "password": "wrong-password"
                                }
                                """))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void loginShouldRejectUnknownUser() throws Exception {
    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                    "username": "unknown-user",
                                    "password": "test-password"
                                }
                                """))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void loginShouldRejectBlockedUser() throws Exception {
    var user = userRepository.findByUsername(USERNAME)
      .orElseThrow();

    user.setStatus(UserStatus.BLOCKED);
    userRepository.saveAndFlush(user);

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginBody()))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointShouldRejectAnonymousUser() throws Exception {
    mockMvc.perform(get("/warehouses"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointShouldRejectInvalidToken() throws Exception {
    mockMvc.perform(get("/warehouses")
        .header("Authorization", "Bearer invalid-token"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointShouldAllowAuthenticatedUser() throws Exception {
    mockMvc.perform(get("/warehouses")
        .header("Authorization", "Bearer " + login()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray());
  }

  private String login() throws Exception {
    String response = mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginBody()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accessToken").isNotEmpty())
      .andReturn()
      .getResponse()
      .getContentAsString();

    return com.jayway.jsonpath.JsonPath.read(
      response, "$.accessToken");
  }

  private String loginBody() {
    return """
                {
                    "username": "auth-test-worker",
                    "password": "test-password"
                }
                """;
  }
}
