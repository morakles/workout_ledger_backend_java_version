package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.GoogleTokenVerifier;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.JwtService;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.GoogleLoginRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.LoginResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.RegisterLocalUserRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public UserDocument registerLocalUser(RegisterLocalUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for local users.");
        }

        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (existing.getProvider() == UserProvider.LOCAL) {
                throw new IllegalArgumentException("Invalid credentials");
            } else {
                throw new IllegalArgumentException("Email is registered with Google. Use Google login instead.");
            }
        });

        UserDocument userDocument = UserDocument.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .provider(UserProvider.LOCAL)
                .roles(List.of("ROLE_USER"))
                .build();

        try {
            UserDocument saved = userRepository.save(userDocument);
            log.info("Registered new LOCAL user: {}", normalizedEmail);
            return saved;
        } catch (DuplicateKeyException e) {
            log.warn("Registration failed: email already exists {}", normalizedEmail);
            throw e;
        }
    }

    public LoginResponse login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        UserDocument user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Authentication failed");
                    return new AuthenticationException("Invalid credentials.");
                });

        if (user.getProvider() != UserProvider.LOCAL) {
            throw new AuthenticationException("Password login is not available for provider: " + user.getProvider());
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed for {}: invalid password", normalizedEmail);
            throw new AuthenticationException("Invalid credentials.");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRoles());
        log.info("Login succeeded for {}", normalizedEmail);
        return new LoginResponse(token, normalizedEmail);
    }

    public LoginResponse googleLogin(GoogleLoginRequest request) {
        String email = googleTokenVerifier.verifyAndExtractEmail(request.idToken());
        String normalizedEmail = normalizeEmail(email);
        String token = processGoogleLogin(normalizedEmail);
        log.info("Google login succeeded for {}", normalizedEmail);
        return new LoginResponse(token, normalizedEmail);
    }

    public String processGoogleLogin(String email) {
        String normalizedEmail = normalizeEmail(email);

        UserDocument user = userRepository.findByEmail(normalizedEmail)
                .map(existing -> {
                    if (existing.getProvider() != UserProvider.GOOGLE) {
                        throw new AuthenticationException("Email is registered as a local account. Use password login.");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    UserDocument newUser = UserDocument.builder()
                            .email(normalizedEmail)
                            .passwordHash(null)
                            .provider(UserProvider.GOOGLE)
                            .roles(List.of("ROLE_USER"))
                            .build();
                    try {
                        return userRepository.save(newUser);
                    } catch (DuplicateKeyException e) {
                        log.info("Google login race detected for {}, fetching existing user", normalizedEmail);
                        return userRepository.findByEmail(normalizedEmail)
                                .orElseThrow(() -> new AuthenticationException("Unable to create Google user."));
                    }
                });

        return jwtService.generateToken(user.getEmail(), user.getRoles());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        return normalized;
    }
}
