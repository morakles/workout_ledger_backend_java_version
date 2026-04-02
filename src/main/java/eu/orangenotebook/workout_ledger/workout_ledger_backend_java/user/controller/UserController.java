package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        try {
            UserDocument createdUser = userService.createUser(request.email(), request.password(), request.provider());
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(createdUser));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam @NotBlank @Email String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public record RegisterUserRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotNull UserProvider provider
    ) {
    }

    public record UserResponse(
            String id,
            String email,
            UserProvider provider,
            Instant createdAt,
            Instant updatedAt,
            List<String> roles
    ) {
        public static UserResponse from(UserDocument userDocument) {
            return new UserResponse(
                    userDocument.getId(),
                    userDocument.getEmail(),
                    userDocument.getProvider(),
                    userDocument.getCreatedAt(),
                    userDocument.getUpdatedAt(),
                    userDocument.getRoles()
            );
        }
    }
}
