package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.GlobalExceptionHandler;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.WorkoutNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.WorkoutService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

        ProxyFactory proxyFactory = new ProxyFactory(new WorkoutController(workoutService));
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new MethodValidationInterceptor((Validator) validator));

        mockMvc = MockMvcBuilders.standaloneSetup(proxyFactory.getProxy())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("create endpoint should return 201 with created workout")
    void createWorkoutSuccess() throws Exception {
        CreateWorkoutRequest request = createWorkoutRequest();
        when(workoutService.createWorkout(USER_EMAIL, request)).thenReturn(workoutResponse());

        mockMvc.perform(post("/api/v1/workouts")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("workout-123"))
                .andExpect(jsonPath("$.name").value("Push day"))
                .andExpect(jsonPath("$.notes").value("Dobry trening"))
                .andExpect(jsonPath("$.workoutDate").value("2026-04-10T06:00:00Z"))
                .andExpect(jsonPath("$.entries[0]").exists())
                .andExpect(jsonPath("$.entries[0].sets[0]").exists())
                .andExpect(jsonPath("$.entries[0].sets[0].type").value(SetType.NORMAL.toString()));


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

        mockMvc.perform(post("/api/v1/workouts")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must not be empty"));

        verifyNoInteractions(workoutService);
    }

    @ParameterizedTest
    @EnumSource(SetType.class)
    @DisplayName("create endpoint should allow zero weight for every performed set type")
    void createWorkoutAllowsZeroWeightForEverySetType(SetType setType) throws Exception {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        null,
                        List.of(setWithZeroWeight(setType))
                ))
        );
        when(workoutService.createWorkout(USER_EMAIL, request)).thenReturn(workoutResponse());

        mockMvc.perform(post("/api/v1/workouts")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(workoutService).createWorkout(USER_EMAIL, request);
    }

    @Test
    @DisplayName("list endpoint should return page metadata")
    void getWorkoutsSuccess() throws Exception {
        when(workoutService.getAll(USER_EMAIL, 1, 10)).thenReturn(workoutPageResponse());

        mockMvc.perform(get("/api/v1/workouts")
                        .principal(authentication())
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("workout-123"))
                .andExpect(jsonPath("$.items[0].notes").value("Dobry trening"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(workoutService).getAll(USER_EMAIL, 1, 10);
    }

    @Test
    @DisplayName("list endpoint should reject negative page")
    void getWorkoutsRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/workouts")
                        .principal(authentication())
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must be greater than or equal to 0"));

        verifyNoInteractions(workoutService);
    }

    @Test
    @DisplayName("list endpoint should reject size below one")
    void getWorkoutsRejectsSizeBelowOne() throws Exception {
        mockMvc.perform(get("/api/v1/workouts")
                        .principal(authentication())
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must be greater than or equal to 1"));

        verifyNoInteractions(workoutService);
    }

    @Test
    @DisplayName("list endpoint should reject size above max")
    void getWorkoutsRejectsSizeAboveMax() throws Exception {
        mockMvc.perform(get("/api/v1/workouts")
                        .principal(authentication())
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must be less than or equal to 100"));

        verifyNoInteractions(workoutService);
    }

    @Test
    @DisplayName("get by id endpoint should return workout")
    void getWorkoutSuccess() throws Exception {
        when(workoutService.getById("workout-123", USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(get("/api/v1/workouts/workout-123")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"))
                .andExpect(jsonPath("$.notes").value("Dobry trening"));

        verify(workoutService).getById("workout-123", USER_EMAIL);
    }

    @Test
    @DisplayName("get by id endpoint should return 404 when workout is missing")
    void getWorkoutNotFound() throws Exception {
        when(workoutService.getById("missing-workout", USER_EMAIL))
                .thenThrow(new WorkoutNotFoundException("Workout not found."));

        mockMvc.perform(get("/api/v1/workouts/missing-workout")
                        .principal(authentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workout not found."));
    }

    @Test
    @DisplayName("put endpoint should update workout")
    void updateWorkoutSuccess() throws Exception {
        CreateWorkoutRequest request = createWorkoutRequest();
        when(workoutService.update("workout-123", request, USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(put("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"));

        verify(workoutService).update("workout-123", request, USER_EMAIL);
    }

    @Test
    @DisplayName("put endpoint should return 404 when workout is missing")
    void updateWorkoutNotFound() throws Exception {
        CreateWorkoutRequest request = createWorkoutRequest();
        when(workoutService.update("missing-workout", request, USER_EMAIL))
                .thenThrow(new WorkoutNotFoundException("Workout not found."));

        mockMvc.perform(put("/api/v1/workouts/missing-workout")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workout not found."));
    }

    @Test
    @DisplayName("put endpoint should reject invalid body")
    void updateWorkoutValidationFailure() throws Exception {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                "Push day",
                null,
                List.of()
        );

        mockMvc.perform(put("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workoutService);
    }

    @Test
    @DisplayName("patch endpoint should partially update workout")
    void patchWorkoutSuccess() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withName("Updated push day", null, null);
        when(workoutService.partialUpdate("workout-123", request, USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(patch("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated push day"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"));

        verify(workoutService).partialUpdate("workout-123", request, USER_EMAIL);
    }

    @Test
    @DisplayName("patch endpoint should pass explicit null name to service")
    void patchWorkoutWithNullName() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withName(null, null, null);
        when(workoutService.partialUpdate("workout-123", request, USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(patch("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"));

        verify(workoutService).partialUpdate("workout-123", request, USER_EMAIL);
    }

    @Test
    @DisplayName("patch endpoint should keep name undefined when name is omitted")
    void patchWorkoutWithOmittedName() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withoutName(
                Instant.parse("2026-04-12T06:00:00Z"),
                null
        );
        when(workoutService.partialUpdate("workout-123", request, USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(patch("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutDate":"2026-04-12T06:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"));

        verify(workoutService).partialUpdate("workout-123", request, USER_EMAIL);
    }

    @Test
    @DisplayName("patch endpoint should update notes")
    void patchWorkoutNotes() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withNotes("Updated notes", null, null);
        when(workoutService.partialUpdate("workout-123", request, USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(patch("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes":"Updated notes"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"));

        verify(workoutService).partialUpdate("workout-123", request, USER_EMAIL);
    }

    @Test
    @DisplayName("patch endpoint should pass explicit null notes to service")
    void patchWorkoutWithNullNotes() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withNotes(null, null, null);
        when(workoutService.partialUpdate("workout-123", request, USER_EMAIL)).thenReturn(workoutResponse());

        mockMvc.perform(patch("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("workout-123"));

        verify(workoutService).partialUpdate("workout-123", request, USER_EMAIL);
    }

    @Test
    @DisplayName("patch endpoint should return 404 when workout is missing")
    void patchWorkoutNotFound() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withName("Updated push day", null, null);
        when(workoutService.partialUpdate("missing-workout", request, USER_EMAIL))
                .thenThrow(new WorkoutNotFoundException("Workout not found."));

        mockMvc.perform(patch("/api/v1/workouts/missing-workout")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated push day"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workout not found."));
    }

    @Test
    @DisplayName("patch endpoint should return 400 for invalid set definition")
    void patchWorkoutInvalidSetDefinition() throws Exception {
        PatchWorkoutRequest request = PatchWorkoutRequest.withoutName(
                null,
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        null,
                        List.of(new CreateSetEntryRequest(1, null, 5, null, null, null, SetType.TIME))
                ))
        );
        when(workoutService.partialUpdate("workout-123", request, USER_EMAIL))
                .thenThrow(new IllegalArgumentException("Set type TIME requires durationSeconds greater than 0."));

        mockMvc.perform(patch("/api/v1/workouts/workout-123")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entries": [
                                    {
                                      "exerciseId": "exercise-123",
                                      "notes": null,
                                      "sets": [
                                        {
                                          "setNumber": 1,
                                          "weight": null,
                                          "reps": 5,
                                          "restSeconds": null,
                                          "durationSeconds": null,
                                          "distanceMeters": null,
                                          "type": "TIME"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Set type TIME requires durationSeconds greater than 0."));
    }

    @Test
    @DisplayName("delete endpoint should return 204")
    void deleteWorkoutSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/workouts/workout-123")
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(workoutService).delete("workout-123", USER_EMAIL);
    }

    @Test
    @DisplayName("delete endpoint should return 404 when workout is missing")
    void deleteWorkoutNotFound() throws Exception {
        doThrow(new WorkoutNotFoundException("Workout not found."))
                .when(workoutService).delete("missing-workout", USER_EMAIL);

        mockMvc.perform(delete("/api/v1/workouts/missing-workout")
                        .principal(authentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workout not found."));
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(USER_EMAIL, "n/a", List.of());
    }

    private CreateWorkoutRequest createWorkoutRequest() {
        return new CreateWorkoutRequest(
                "Push day",
                "Dobry trening",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        "Dobre czucie",
                        List.of(
                                new CreateSetEntryRequest(1, 60.0, 10, null, 60.0, null, SetType.NORMAL),
                                new CreateSetEntryRequest(2, 62.5, 8, null, 60.0, null, SetType.NORMAL)
                        )
                ))
        );
    }

    private WorkoutResponse workoutResponse() {
        return new WorkoutResponse(
                "workout-123",
                "Push day",
                "Dobry trening",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new WorkoutEntryResponse(
                        "exercise-123",
                        "Dobre czucie",
                        List.of(
                                new SetEntryResponse(1, 60.0, 10, null, null, null, SetType.NORMAL),
                                new SetEntryResponse(2, 62.5, 8, null, null, null, SetType.NORMAL)
                        )
                )),
                Instant.parse("2026-04-10T07:00:00Z"),
                Instant.parse("2026-04-10T07:00:00Z")
        );
    }

    private WorkoutPageResponse workoutPageResponse() {
        return new WorkoutPageResponse(
                List.of(workoutResponse()),
                1,
                10,
                21,
                3,
                true,
                true
        );
    }

    private CreateSetEntryRequest setWithZeroWeight(SetType setType) {
        return switch (setType) {
            case TIME -> new CreateSetEntryRequest(1, 0.0, null, null, 30.0, null, setType);
            case DISTANCE -> new CreateSetEntryRequest(1, 0.0, null, null, null, 400.0, setType);
            case NORMAL, WARMUP, DROP, FAILURE ->
                    new CreateSetEntryRequest(1, 0.0, 10, null, null, null, setType);
        };
    }
}
