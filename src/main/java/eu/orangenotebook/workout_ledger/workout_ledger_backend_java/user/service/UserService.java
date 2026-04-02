package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
