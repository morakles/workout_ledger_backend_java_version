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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        UserDocument createdUser = userService.createUser(request.email(), request.password(), request.provider());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(createdUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google-login")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = userService.googleLogin(request.idToken());
        return ResponseEntity.ok(response);
    }

    public record RegisterUserRequest(
            @NotBlank @Email String email,
            String password,
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
