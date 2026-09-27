package com.skr1l.backend.user;

import com.skr1l.backend.exception.userException.UserAlreadyExistsException;
import com.skr1l.backend.exception.userException.UserNotFoundException;
import com.skr1l.backend.user.dto.UserRequestDTO;
import com.skr1l.backend.user.dto.UserResponseDTO;
import com.skr1l.backend.user.enums.UserStatus;
import com.skr1l.backend.user.repository.UserRepository;
import com.skr1l.backend.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequestDTO userDto;

    @BeforeEach
    void setUp() {
        userDto = new UserRequestDTO(
                "testemail@gmail.com",
                "testUsername",
                "testPassword"
        );
    }

    @Test
    void createUserShouldReturnIdWhenUserIsValid() {
        //Arrange

        when(userRepository.existsByEmail(userDto.email()))
                .thenReturn(false);

        when(userRepository.existsByUsername(userDto.username()))
        .thenReturn(false);

        when(passwordEncoder.encode(userDto.password()))
        .thenReturn("encodedTestPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->{
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        //Act
        Long result = userService.createUser(userDto);

        //Assert
        assertEquals(1L, result);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(userDto.password());
    }

    @Test
    void createUserShouldThrowExceptionWhenUserExistsByEmail() {
        //Arrange

        when(userRepository.existsByEmail(userDto.email()))
                .thenReturn(true);
        //Act
        //Assert
        assertThrows(UserAlreadyExistsException.class, () ->
                userService.createUser(userDto));
    }

    @Test
    void createUserShouldThrowExceptionWhenUserExistsByUsername() {
        //Arrange

        when(userRepository.existsByUsername(userDto.username()))
        .thenReturn(true);
        //Act
        //Assert
        assertThrows(UserAlreadyExistsException.class, () ->
                userService.createUser(userDto));
    }

    @Test
    void getUserByEmailShouldReturnUserWhenUserIsValid() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail(userDto.email());
        user.setUsername(userDto.username());

        when(userRepository.findByEmail(userDto.email()))
                .thenReturn(Optional.of(user));
        //Act
        User result = userService.getUserByEmail(userDto.email());

        //Assert
        assertEquals(user, result);
        verify(userRepository).findByEmail(userDto.email());
    }

    @Test
    void getUserByEmailShouldThrowExceptionWhenUserIsNotExistsByEmail() {
        //Arrange
        when(userRepository.findByEmail(userDto.email()))
        .thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.getUserByEmail(userDto.email()));
        verify(userRepository).findByEmail(userDto.email());
    }

    @Test
    void getUserByIdShouldReturnUserWhenUserIsValid() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail(userDto.email());
        user.setUsername(userDto.username());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //Act
        UserResponseDTO result = userService.getUserById(1L);

        //Assert
        assertEquals(1L, result.id());
        assertEquals(userDto.email(), result.email());
        assertEquals(userDto.username(), result.username());
    }

    @Test
    void getUserByIdShouldThrowExceptionIfUserWithIdDoesntExist() {

        //Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void getListOfUsersShouldReturnListOfUsersWhenUserIsValid() {
        //Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@gmail.com");
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(1L);
        user2.setEmail("user2@gmail.com");
        user2.setUsername("user2");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        //Act
        List<UserResponseDTO> result = userService.getAllUsers();

        //Assert
        assertEquals(2, result.size());
        assertEquals("user1@gmail.com", result.get(0).email());
        assertEquals("user2@gmail.com", result.get(1).email());
        verify(userRepository).findAll();
    }

    @Test
    void getListOfUsersShouldReturnEmptyListWhenDoesntHasUsers() {
        //Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        //Act
        List<UserResponseDTO> result = userService.getAllUsers();

        //Assert
        assertEquals(0, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void updateUserShouldReturnUpdatedUserWhenUserIsValid() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("user1@gmail.com");
        user.setUsername("user1");
        user.setPasswordHash("password");

        UserRequestDTO result = new UserRequestDTO(
                "newUserEmail@gmail.com",
                "newUsername",
                "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(passwordEncoder.encode(result.password()))
                .thenReturn("encodedPassword");

        //Act
        userService.updateUser(1L, result);

        //Assert
        assertEquals("newUserEmail@gmail.com", user.getEmail());
        assertEquals("newUsername", user.getUsername());
        assertEquals("encodedPassword", user.getPasswordHash());

        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode(result.password());
    }

    @Test
    void updateUserShouldThrowExceptionWhenUserDoesntExists() {
        //Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(1L, userDto));
    }

    @Test
    void updateUserShouldThrowExceptionWhenUserWithThisNameAlreadyExists() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("user1@gmail.com");
        user.setUsername("user1");
        user.setPasswordHash("password");

        UserRequestDTO result = new UserRequestDTO(
                "newUserEmail@gmail.com",
                "newUsername",
                "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(result.username()))
                .thenReturn(true);

        //Act
        //Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> userService.updateUser(1L, result));
    }

    @Test
    void updateUserShouldThrowExceptionWhenUserWithThisEmailAlreadyExists() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("user1@gmail.com");
        user.setUsername("user1");
        user.setPasswordHash("password");

        UserRequestDTO result = new UserRequestDTO(
                "newUserEmail@gmail.com",
                "newUsername",
                "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(result.email()))
                .thenReturn(true);

        //Act
        //Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> userService.updateUser(1L, result));
    }

    @Test
    void updateUserShouldThrowExceptionWhenUserDoesNotExist() {
        //Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(1L, userDto));
    }

    @Test
    void blockUserShouldSetBlockedToUserWhenUserIsValid() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("user1@gmail.com");
        user.setUsername("user1");
        user.setPasswordHash("password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        //Act
        userService.blockUser(1L);

        //Assert
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        verify(userRepository).findById(1L);
    }

    @Test
    void blockUserShouldThrowExceptionWhenUserIsInvalid() {
        //Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.blockUser(1L));
    }

    @Test
    void unblockUserShouldSetActiveToUserWhenUserIsValid() {
        //Arrange
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        //Act
        userService.unblockUser(1L);

        //Assert
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).findById(1L);
    }

    @Test
    void unblockUserShouldThrowExceptionWhenUserIsInvalid() {
        //Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.unblockUser(1L));
    }

    @Test
    void deleteUserShouldDeleteUserWhenUserIsValid() {
        //Arrange
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        //Act
        userService.deleteUser(1L);

        //Assert
        verify(userRepository).findById(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserShouldThrowExceptionWhenUserDoesNotExist() {
        //Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        //Act
        //Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser(1L));

        verify(userRepository).findById(1L);
    }
}
