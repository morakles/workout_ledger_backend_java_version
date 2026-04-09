package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.UserAlreadyExistsException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.GoogleTokenVerifier;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.JwtService;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.RegisterLocalUserRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    JwtService jwtService;

    @Mock
    GoogleTokenVerifier googleTokenVerifier;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("should throw domain conflict when local user already exists before save")
    void registerLocalUserDuplicatePrecheck() {
        UserService userService = new UserService(userRepository, passwordEncoder, jwtService, googleTokenVerifier);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(UserDocument.builder()
                .id("user-1")
                .email("user@email.com")
                .provider(UserProvider.LOCAL)
                .passwordHash("hash")
                .roles(List.of("ROLE_USER"))
                .build()));

        assertThatThrownBy(() -> userService.registerLocalUser(
                new RegisterLocalUserRequest("user@email.com", "password123")
        ))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User with this email already exists.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should translate duplicate key race during registration into domain conflict")
    void registerLocalUserDuplicateKeyRace() {
        UserService userService = new UserService(userRepository, passwordEncoder, jwtService, googleTokenVerifier);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserDocument.class))).thenThrow(new DuplicateKeyException("duplicate user"));

        assertThatThrownBy(() -> userService.registerLocalUser(
                new RegisterLocalUserRequest("user@email.com", "password123")
        ))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User with this email already exists.");
    }
}
