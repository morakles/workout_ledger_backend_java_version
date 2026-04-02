package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.LoginResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.GoogleTokenVerifier;
import lombok.RequiredArgsConstructor;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public UserDocument createUser(String email, String password, UserProvider provider) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (provider == null) {
            throw new IllegalArgumentException("Provider is required.");
        }

        String passwordHash = null;

        if (provider == UserProvider.LOCAL) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("Password is required for local users.");
            }
            passwordHash = passwordEncoder.encode(password);
        } else if (provider == UserProvider.GOOGLE) {
            passwordHash = null;
        } else {
            throw new IllegalArgumentException("Unsupported provider.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("A user with this email already exists.");
        }

        UserDocument userDocument = UserDocument.builder()
                .email(email)
                .passwordHash(passwordHash)
                .provider(provider)
                .roles(List.of("ROLE_USER"))
                .build();

        return userRepository.save(userDocument);
    }

    public Optional<UserDocument> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        UserDocument user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Invalid credentials."));

        if (user.getProvider() != UserProvider.LOCAL) {
            throw new IllegalArgumentException("Password login is not available for provider: " + user.getProvider());
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalStateException("Invalid credentials.");
        }

        return jwtService.generateToken(user.getEmail(), user.getRoles());
    }

    public LoginResponse googleLogin(String idToken) {
        String email = googleTokenVerifier.verifyAndExtractEmail(idToken);
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Invalid Google ID token.");
        }
        String token = processGoogleLogin(email);
        return new LoginResponse(token, email);
    }

    public String processGoogleLogin(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        UserDocument user = userRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getProvider() != UserProvider.GOOGLE) {
                        throw new IllegalStateException("Email is registered with a different provider.");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    UserDocument newUser = UserDocument.builder()
                            .email(email)
                            .passwordHash(null)
                            .provider(UserProvider.GOOGLE)
                            .roles(List.of("ROLE_USER"))
                            .build();
                    return userRepository.save(newUser);
                });

        return jwtService.generateToken(user.getEmail(), user.getRoles());
    }
}
