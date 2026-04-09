package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.GoogleLoginRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.LoginRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller.RegisterLocalUserRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude=" +
        "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    ExerciseRepository exerciseRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.GoogleTokenVerifier googleTokenVerifier;

    Map<String, UserDocument> fakeDb;

    @BeforeEach
    void clean() {
        fakeDb = new ConcurrentHashMap<>();

        Mockito.lenient().when(userRepository.findByEmail(anyString()))
                .thenAnswer(inv -> {
                    String email = ((String) inv.getArgument(0)).toLowerCase();
                    return java.util.Optional.ofNullable(fakeDb.get(email));
                });

        Mockito.lenient().when(userRepository.existsByEmail(anyString()))
                .thenAnswer(inv -> {
                    String email = ((String) inv.getArgument(0)).toLowerCase();
                    return fakeDb.containsKey(email);
                });

        Mockito.lenient().when(userRepository.save(any()))
                .thenAnswer(inv -> {
                    UserDocument doc = inv.getArgument(0);
                    if (doc.getId() == null) {
                        doc = UserDocument.builder()
                                .id(UUID.randomUUID().toString())
                                .email(doc.getEmail())
                                .passwordHash(doc.getPasswordHash())
                                .provider(doc.getProvider())
                                .roles(doc.getRoles())
                                .build();
                    }
                    fakeDb.put(doc.getEmail(), doc);
                    return doc;
                });

        Mockito.doAnswer(inv -> {
            fakeDb.clear();
            return null;
        }).when(userRepository).deleteAll();
    }

    @Nested
    class Register {
        @Test
        @DisplayName("should register LOCAL user successfully")
        void registerLocalSuccess() throws Exception {
            RegisterLocalUserRequest req = new RegisterLocalUserRequest("User@Email.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("user@email.com"))
                    .andExpect(jsonPath("$.provider").value("LOCAL"));

            UserDocument saved = userRepository.findByEmail("user@email.com").orElseThrow();
            assertThat(saved.getProvider()).isEqualTo(UserProvider.LOCAL);
            assertThat(saved.getPasswordHash()).isNotBlank();
            assertThat(saved.getPasswordHash()).doesNotContain("password123");
        }

        @Test
        @DisplayName("should reject blank password")
        void registerBlankPassword() throws Exception {
            RegisterLocalUserRequest req = new RegisterLocalUserRequest("user@email.com", "");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject blank email")
        void registerBlankEmail() throws Exception {
            RegisterLocalUserRequest req = new RegisterLocalUserRequest("", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject invalid email format")
        void registerInvalidEmail() throws Exception {
            RegisterLocalUserRequest req = new RegisterLocalUserRequest("not-an-email", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject duplicate LOCAL email")
        void registerDuplicateLocal() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("user@email.com")
                    .provider(UserProvider.LOCAL)
                    .passwordHash("hash")
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());

            RegisterLocalUserRequest req = new RegisterLocalUserRequest("user@email.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("User with this email already exists."));
        }

        @Test
        @DisplayName("should translate duplicate key race during register into conflict")
        void registerDuplicateKeyRace() throws Exception {
            Mockito.doThrow(new DuplicateKeyException("duplicate user"))
                    .when(userRepository).save(any(UserDocument.class));

            RegisterLocalUserRequest req = new RegisterLocalUserRequest("user@email.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("User with this email already exists."));
        }

        @Test
        @DisplayName("should report provider conflict when GOOGLE exists")
        void registerProviderConflict() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("user@email.com")
                    .provider(UserProvider.GOOGLE)
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());

            RegisterLocalUserRequest req = new RegisterLocalUserRequest("user@email.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Login {
        @Test
        @DisplayName("should login LOCAL user and return token")
        void loginSuccess() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("user@email.com")
                    .provider(UserProvider.LOCAL)
                    .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"))
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());

            LoginRequest req = new LoginRequest("user@email.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("user@email.com"));
        }

        @Test
        @DisplayName("should reject wrong password")
        void loginWrongPassword() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("user@email.com")
                    .provider(UserProvider.LOCAL)
                    .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"))
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());

            LoginRequest req = new LoginRequest("user@email.com", "wrong");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject login for GOOGLE account")
        void loginGoogleAccountWithPassword() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("user@email.com")
                    .provider(UserProvider.GOOGLE)
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());

            LoginRequest req = new LoginRequest("user@email.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject unknown email")
        void loginUnknownEmail() throws Exception {
            LoginRequest req = new LoginRequest("missing@email.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should normalize email on login")
        void loginNormalizesEmail() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("user@email.com")
                    .provider(UserProvider.LOCAL)
                    .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"))
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());

            LoginRequest req = new LoginRequest("User@Email.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@email.com"));
        }
    }

    @Nested
    class GoogleLogin {
        @Test
        @DisplayName("should login existing GOOGLE user and return token")
        void googleLoginExistingUser() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("googleuser@email.com")
                    .provider(UserProvider.GOOGLE)
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());
            when(googleTokenVerifier.verifyAndExtractEmail(anyString())).thenReturn("googleuser@email.com");

            GoogleLoginRequest req = new GoogleLoginRequest("id-token");

            mockMvc.perform(post("/api/auth/google-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("googleuser@email.com"));
        }

        @Test
        @DisplayName("should create new GOOGLE user on first login")
        void googleLoginCreatesUser() throws Exception {
            when(googleTokenVerifier.verifyAndExtractEmail(anyString())).thenReturn("newgoogle@email.com");

            GoogleLoginRequest req = new GoogleLoginRequest("id-token");

            mockMvc.perform(post("/api/auth/google-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("newgoogle@email.com"));

            UserDocument saved = userRepository.findByEmail("newgoogle@email.com").orElseThrow();
            assertThat(saved.getProvider()).isEqualTo(UserProvider.GOOGLE);
        }

        @Test
        @DisplayName("should reject google login when LOCAL account exists")
        void googleLoginProviderConflict() throws Exception {
            userRepository.save(UserDocument.builder()
                    .email("conflict@email.com")
                    .provider(UserProvider.LOCAL)
                    .passwordHash("hash")
                    .roles(java.util.List.of("ROLE_USER"))
                    .build());
            when(googleTokenVerifier.verifyAndExtractEmail(anyString())).thenReturn("conflict@email.com");

            GoogleLoginRequest req = new GoogleLoginRequest("id-token");

            mockMvc.perform(post("/api/auth/google-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject invalid google token")
        void googleLoginInvalidToken() throws Exception {
            when(googleTokenVerifier.verifyAndExtractEmail(anyString()))
                    .thenThrow(new eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException("Google token verification failed"));

            GoogleLoginRequest req = new GoogleLoginRequest("bad-token");

            mockMvc.perform(post("/api/auth/google-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
