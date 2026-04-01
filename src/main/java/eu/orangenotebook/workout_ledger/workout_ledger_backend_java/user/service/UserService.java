package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDocument createUser(String email, String password, UserProvider provider) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        // TODO: Replace with a secure password hashing implementation (e.g., BCrypt).
        String passwordHash = password;

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
