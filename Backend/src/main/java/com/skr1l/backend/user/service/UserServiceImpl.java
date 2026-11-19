package com.skr1l.backend.user.service;

import com.skr1l.backend.exception.userExceptions.UserAlreadyExistsException;
import com.skr1l.backend.exception.userExceptions.UserNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.user.User;
import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.dto.UserResponseDTO;
import com.skr1l.backend.user.enums.UserRole;
import com.skr1l.backend.user.enums.UserStatus;
import com.skr1l.backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final static AppLogger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long createUser(UserRequestDTO userDto) {
        log.info("createUser");
        Objects.requireNonNull(userDto, "userDto is null");

        if (userRepository.existsByEmail(userDto.email())) {
            throw new UserAlreadyExistsException("User with that email already exists");
        }

        if (userRepository.existsByUsername(userDto.username())) {
            throw new UserAlreadyExistsException("User with that username already exists");
        }

        User user = new User();
        user.setEmail(userDto.email());
        user.setUsername(userDto.username());
        user.setPasswordHash(passwordEncoder.encode(userDto.password()));
        user.setRole(UserRole.WORKER);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        return user.getId();
    }

    @Override
    public User getUserByEmail(String email) {
        log.info("getUserByEmail");
        Objects.requireNonNull(email, "email is null");

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User is not found"));
    }

    @Override
    public User getUserByUsername(String username) {
        log.info("getUserByUsername");
        Objects.requireNonNull(username, "username is null");

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User is not found"));
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        log.info("getUserById");
        return toResponseDTO(getUserEntityById(id));
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        log.info("getAllUsers");

        return userRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private User getUserEntityById(Long id) {
        log.info("getUserEntityById");
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User is not found"));
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO userDto) {
        log.info("updateUser");
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(userDto, "userDto is null");

        User user = getUserEntityById(id);

        if (userRepository.existsByUsername(userDto.username())
                && !user.getUsername().equals(userDto.username())) {
            throw new UserAlreadyExistsException("User with that username already exists");
        }

        if (userRepository.existsByEmail(userDto.email())
                && !user.getEmail().equals(userDto.email())) {
            throw new UserAlreadyExistsException("User with that email already exists");
        }

        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setPasswordHash(passwordEncoder.encode(userDto.password()));

        return toResponseDTO(user);
    }

    @Override
    @Transactional
    public void blockUser(Long id) {
        log.info("blockUser");
        User user = getUserEntityById(id);
        user.setStatus(UserStatus.BLOCKED);
    }

    @Override
    @Transactional
    public void unblockUser(Long id) {
        log.info("unblockUser");
        User user = getUserEntityById(id);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("deleteUser");
        User user = getUserEntityById(id);
        userRepository.delete(user);
    }

    private UserResponseDTO toResponseDTO(User user) {
        log.info("toResponseDTO");
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.getStatus()
        );
    }
}
