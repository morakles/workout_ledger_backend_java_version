package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseAlreadyExistsException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.GlobalExceptionHandler;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseService;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExerciseControllerTest {

    private static final String USER_EMAIL = "user@email.com";

    @Mock
    ExerciseService exerciseService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ExerciseController(exerciseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("get endpoint should return exercise list items")
    void getExercisesSuccess() throws Exception {
        List<ExerciseDocument> exercises = List.of(
                ExerciseDocument.builder()
                        .id("exercise-1")
                        .userId("user-1")
                        .name("Bench Press")
                        .category("CHEST")
                        .build(),
                ExerciseDocument.builder()
                        .id("exercise-2")
                        .userId("user-1")
                        .name("Squat")
                        .category("LEGS")
                        .build()
        );
        when(exerciseService.listExercises(USER_EMAIL)).thenReturn(exercises);

        mockMvc.perform(get("/api/exercises")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("exercise-1"))
                .andExpect(jsonPath("$[0].name").value("Bench Press"))
                .andExpect(jsonPath("$[0].category").value("CHEST"))
                .andExpect(jsonPath("$[1].id").value("exercise-2"))
                .andExpect(jsonPath("$[1].name").value("Squat"))
                .andExpect(jsonPath("$[1].category").value("LEGS"));

        verify(exerciseService).listExercises(USER_EMAIL);
        verifyNoMoreInteractions(exerciseService);
    }

    @Test
    @DisplayName("update endpoint should return 200 with updated exercise")
    void updateExerciseSuccess() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest focus");
        ExerciseDocument updatedExercise = ExerciseDocument.builder()
                .id("exercise-1")
                .userId("user-1")
                .name("Incline Bench Press")
                .category("CHEST")
                .description("Upper chest focus")
                .createdAt(Instant.parse("2026-04-07T12:00:00Z"))
                .updatedAt(Instant.parse("2026-04-08T12:00:00Z"))
                .build();
        when(exerciseService.updateExercise(USER_EMAIL, "exercise-1", request)).thenReturn(updatedExercise);

        mockMvc.perform(put("/api/exercises/exercise-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("exercise-1"))
                .andExpect(jsonPath("$.name").value("Incline Bench Press"))
                .andExpect(jsonPath("$.category").value("CHEST"))
                .andExpect(jsonPath("$.description").value("Upper chest focus"));

        verify(exerciseService).updateExercise(USER_EMAIL, "exercise-1", request);
    }

    @Test
    @DisplayName("update endpoint should return 404 when exercise is missing")
    void updateExerciseMissing() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest focus");
        when(exerciseService.updateExercise(USER_EMAIL, "missing", request))
                .thenThrow(new ExerciseNotFoundException("Exercise not found."));

        mockMvc.perform(put("/api/exercises/missing")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));
    }

    @Test
    @DisplayName("update endpoint should return 404 when exercise belongs to another user")
    void updateExerciseForDifferentUser() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest focus");
        when(exerciseService.updateExercise(USER_EMAIL, "foreign-exercise", request))
                .thenThrow(new ExerciseNotFoundException("Exercise not found."));

        mockMvc.perform(put("/api/exercises/foreign-exercise")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));
    }

    @Test
    @DisplayName("update endpoint should return 409 when new exercise name conflicts")
    void updateExerciseDuplicateName() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest focus");
        when(exerciseService.updateExercise(USER_EMAIL, "exercise-1", request))
                .thenThrow(new ExerciseAlreadyExistsException("Exercise with this name already exists."));

        mockMvc.perform(put("/api/exercises/exercise-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exercise with this name already exists."));
    }

    @Test
    @DisplayName("update endpoint should reject invalid payload")
    void updateExerciseValidationFailure() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest(" ", "CHEST", "Upper chest focus");

        mockMvc.perform(put("/api/exercises/exercise-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must not be blank"));

        verifyNoInteractions(exerciseService);
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(USER_EMAIL, "n/a", List.of());
    }
}
