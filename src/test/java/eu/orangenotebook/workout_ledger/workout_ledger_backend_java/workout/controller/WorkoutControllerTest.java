package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.GlobalExceptionHandler;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.WorkoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkoutControllerTest {

    private static final String USER_EMAIL = "user@email.com";

    @Mock
    WorkoutService workoutService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new WorkoutController(workoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("create endpoint should return 201 with created workout")
    void createWorkoutSuccess() throws Exception {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        "Dobre czucie",
                        List.of(
                                new CreateSetEntryRequest(1, 60.0, 10, 60, 60.0, (double) 00, SetType.NORMAL),
                                new CreateSetEntryRequest(2, 62.5, 8, 60, 60.0, (double) 00, SetType.NORMAL)
                        )
                ))
        );
        WorkoutResponse response = new WorkoutResponse(
                "workout-123",
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new WorkoutEntryResponse(
                        "exercise-123",
                        "Dobre czucie",
                        List.of(
                                new SetEntryResponse(1, 60.0, 10, null, 60.0, (double) 00, SetType.NORMAL),
                                new SetEntryResponse(2, 62.5, 8, null, 60.0, (double) 00, SetType.NORMAL)
                        )
                )),
                Instant.parse("2026-04-10T07:00:00Z"),
                Instant.parse("2026-04-10T07:00:00Z")
        );
        when(workoutService.createWorkout(USER_EMAIL, request)).thenReturn(response);

        mockMvc.perform(post("/api/workouts")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("workout-123"))
                .andExpect(jsonPath("$.name").value("Push day"))
                .andExpect(jsonPath("$.workoutDate").value("2026-04-10T06:00:00Z"))
                .andExpect(jsonPath("$.entries[0].exerciseId").value("exercise-123"))
                .andExpect(jsonPath("$.entries[0].notes").value("Dobre czucie"))
                .andExpect(jsonPath("$.entries[0].sets[0].setNumber").value(1))
                .andExpect(jsonPath("$.entries[0].sets[0].weight").value(60.0))
                .andExpect(jsonPath("$.entries[0].sets[0].reps").value(10));

        verify(workoutService).createWorkout(USER_EMAIL, request);
    }

    @Test
    @DisplayName("create endpoint should reject invalid payload")
    void createWorkoutValidationFailure() throws Exception {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of()
        );

        mockMvc.perform(post("/api/workouts")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must not be empty"));

        verifyNoInteractions(workoutService);
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(USER_EMAIL, "n/a", List.of());
    }
}
