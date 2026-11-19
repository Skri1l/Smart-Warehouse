package com.skr1l.backend.user;

import com.skr1l.backend.user.enums.UserRole;
import com.skr1l.backend.user.enums.UserStatus;
import com.skr1l.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@gmail.com");
        user.setUsername("testUser");
        user.setPasswordHash("encodedPassword");
        user.setRole(UserRole.WORKER);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
    }

    @Test
    void findByUsernameShouldReturnUserWhenUserExists() {
        Optional<User> result =
                userRepository.findByUsername("testUser");

        assertTrue(result.isPresent());
        assertEquals("testUser", result.get().getUsername());
    }

    @Test
    void findByUsernameShouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> result =
                userRepository.findByUsername("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmailShouldReturnUserWhenUserExists() {
        Optional<User> result =
                userRepository.findByEmail("test@gmail.com");

        assertTrue(result.isPresent());
        assertEquals("test@gmail.com", result.get().getEmail());
    }

    @Test
    void existsByUsernameShouldReturnTrueWhenUserExists() {
        boolean result =
                userRepository.existsByUsername("testUser");

        assertTrue(result);
    }

    @Test
    void existsByUsernameShouldReturnFalseWhenUserDoesNotExist() {
        boolean result =
                userRepository.existsByUsername("unknown");

        assertFalse(result);
    }

    @Test
    void existsByEmailShouldReturnTrueWhenUserExists() {
        boolean result =
                userRepository.existsByEmail("test@gmail.com");

        assertTrue(result);
    }

    @Test
    void existsByEmailShouldReturnFalseWhenUserDoesNotExist() {
        boolean result =
                userRepository.existsByEmail("unknown@gmail.com");

        assertFalse(result);
    }
}