package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.config.ApiPaths;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.service.UserService;
import jakarta.validation.Valid;
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
@RequestMapping(ApiPaths.API_V1 + "/auth")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterLocalUserRequest request) {
        UserDocument createdUser = userService.registerLocalUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(createdUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google-login")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = userService.googleLogin(request);
        return ResponseEntity.ok(response);
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
