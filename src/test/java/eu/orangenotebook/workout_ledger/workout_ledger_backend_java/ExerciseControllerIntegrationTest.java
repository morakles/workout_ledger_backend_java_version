package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller.CreateExerciseRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller.UpdateExerciseRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseReferenceChecker;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository.TrainingPlanRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.autoconfigure.exclude=" +
        "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExerciseControllerIntegrationTest {

    private static final String USER_EMAIL = "user@email.com";
    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_EMAIL = "other@email.com";
    private static final String OTHER_USER_ID = "user-2";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.JwtService jwtService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    ExerciseRepository exerciseRepository;

    @MockitoBean
    ExerciseReferenceChecker exerciseReferenceChecker;

    @MockitoBean
    WorkoutRepository workoutRepository;

    @MockitoBean
    TrainingPlanRepository trainingPlanRepository;

    Map<String, UserDocument> usersByEmail;
    Map<String, ExerciseDocument> exercisesById;

    @BeforeEach
    void setUp() {
        usersByEmail = new ConcurrentHashMap<>();
        exercisesById = new ConcurrentHashMap<>();

        usersByEmail.put(USER_EMAIL, createUser(USER_ID, USER_EMAIL));
        usersByEmail.put(OTHER_USER_EMAIL, createUser(OTHER_USER_ID, OTHER_USER_EMAIL));

        Mockito.lenient().when(userRepository.findByEmail(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(usersByEmail.get(invocation.getArgument(0))));

        Mockito.lenient().when(exerciseRepository.existsByUserIdAndNameNormalized(anyString(), anyString()))
                .thenAnswer(invocation -> exercisesById.values().stream()
                        .anyMatch(exercise -> exercise.getUserId().equals(invocation.getArgument(0))
                                && exercise.getNameNormalized().equals(invocation.getArgument(1))));

        Mockito.lenient().when(exerciseRepository.save(any(ExerciseDocument.class)))
                .thenAnswer(invocation -> {
                    ExerciseDocument exercise = invocation.getArgument(0);
                    boolean duplicateExists = exercisesById.values().stream()
                            .filter(existing -> !Objects.equals(existing.getId(), exercise.getId()))
                            .anyMatch(existing -> existing.getUserId().equals(exercise.getUserId())
                                    && existing.getNameNormalized().equals(exercise.getNameNormalized()));
                    if (duplicateExists) {
                        throw new DuplicateKeyException("duplicate exercise");
                    }

                    Instant createdAt = exercise.getCreatedAt() != null
                            ? exercise.getCreatedAt()
                            : Instant.parse("2026-04-07T12:00:00Z");
                    ExerciseDocument saved = ExerciseDocument.builder()
                            .id(exercise.getId() != null ? exercise.getId() : UUID.randomUUID().toString())
                            .userId(exercise.getUserId())
                            .name(exercise.getName())
                            .category(exercise.getCategory())
                            .description(exercise.getDescription())
                            .createdAt(createdAt)
                            .updatedAt(Instant.parse("2026-04-08T12:00:00Z"))
                            .build();
                    exercisesById.put(saved.getId(), saved);
                    return saved;
                });

        Mockito.lenient().when(exerciseRepository.findAllByUserId(anyString(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    String userId = invocation.getArgument(0);
                    Pageable pageable = invocation.getArgument(1);

                    List<ExerciseDocument> filtered = exercisesById.values().stream()
                            .filter(exercise -> exercise.getUserId().equals(userId))
                            .sorted(comparatorFor(pageable))
                            .toList();

                    int start = Math.min((int) pageable.getOffset(), filtered.size());
                    int end = Math.min(start + pageable.getPageSize(), filtered.size());

                    return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
                });

        Mockito.lenient().when(exerciseRepository.findAllByUserIdOrderByNameNormalizedAsc(anyString()))
                .thenAnswer(invocation -> exercisesById.values().stream()
                        .filter(exercise -> exercise.getUserId().equals(invocation.getArgument(0)))
                        .sorted(Comparator.comparing(ExerciseDocument::getNameNormalized))
                        .toList());

        Mockito.lenient().when(exerciseRepository.findByIdAndUserId(anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(exercisesById.get(invocation.getArgument(0)))
                        .filter(exercise -> exercise.getUserId().equals(invocation.getArgument(1))));

        Mockito.lenient().doAnswer(invocation -> {
            ExerciseDocument exercise = invocation.getArgument(0);
            exercisesById.remove(exercise.getId());
            return null;
        }).when(exerciseRepository).delete(any(ExerciseDocument.class));

        Mockito.lenient().when(exerciseReferenceChecker.isExerciseInUse(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("should create exercise for authenticated user")
    void createExerciseSuccess() throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest(" Bench Press ", " CHEST ", null);

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bench Press"))
                .andExpect(jsonPath("$.category").value("CHEST"));

        ExerciseDocument saved = exercisesById.values().stream()
                .filter(exercise -> exercise.getUserId().equals(USER_ID))
                .filter(exercise -> exercise.getNameNormalized().equals("bench press"))
                .findFirst()
                .orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getName()).isEqualTo("Bench Press");
        assertThat(saved.getNameNormalized()).isEqualTo("bench press");
        assertThat(saved.getCategory()).isEqualTo("CHEST");
    }

    @Test
    @DisplayName("should reject duplicate normalized name for the same user")
    void createExerciseDuplicateForSameUser() throws Exception {
        insertExercise("existing", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));

        CreateExerciseRequest request = new CreateExerciseRequest("  BENCH PRESS  ", "CHEST", null);

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exercise with this name already exists."));
    }

    @Test
    @DisplayName("should allow same normalized name for a different user")
    void createExerciseAllowsSameNameForDifferentUser() throws Exception {
        insertExercise("existing", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));

        CreateExerciseRequest request = new CreateExerciseRequest("bench press", "CHEST", null);

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearerToken(OTHER_USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("bench press"));

        assertThat(exercisesById.values().stream()
                .filter(exercise -> exercise.getNameNormalized().equals("bench press"))
                .map(ExerciseDocument::getUserId)
                .toList()).containsExactlyInAnyOrder(USER_ID, OTHER_USER_ID);
    }

    @Test
    @DisplayName("should reject blank exercise name")
    void createExerciseBlankName() throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest(" ", "CHEST", null);

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should translate duplicate key race into conflict")
    void createExerciseDuplicateKeyRace() throws Exception {
        Mockito.when(exerciseRepository.save(any(ExerciseDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate exercise"));

        CreateExerciseRequest request = new CreateExerciseRequest("Bench Press", "CHEST", null);

        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exercise with this name already exists."));
    }

    @Test
    @DisplayName("should list only authenticated users exercises")
    void getExercisesReturnsAuthenticatedUserExercises() throws Exception {
        insertExercise("exercise-1", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));
        insertExercise("exercise-2", USER_ID, "Squat", "LEGS", Instant.parse("2026-04-06T12:00:00Z"));
        insertExercise("exercise-3", OTHER_USER_ID, "Deadlift", "BACK", Instant.parse("2026-04-07T12:00:00Z"));

        mockMvc.perform(get("/api/v1/exercises")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("exercise-1"))
                .andExpect(jsonPath("$[0].name").value("Bench Press"))
                .andExpect(jsonPath("$[0].category").value("CHEST"))
                .andExpect(jsonPath("$[1].id").value("exercise-2"))
                .andExpect(jsonPath("$[1].name").value("Squat"))
                .andExpect(jsonPath("$[1].category").value("LEGS"));
    }

    @Test
    @DisplayName("should sort exercises by normalized name ascending")
    void getExercisesSortsResults() throws Exception {
        insertExercise("exercise-1", USER_ID, "squat", "LEGS", Instant.parse("2026-04-05T12:00:00Z"));
        insertExercise("exercise-2", USER_ID, " Bench Press ", "CHEST", Instant.parse("2026-04-07T12:00:00Z"));
        insertExercise("exercise-3", USER_ID, "deadlift", "BACK", Instant.parse("2026-04-06T12:00:00Z"));

        mockMvc.perform(get("/api/v1/exercises")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bench Press"))
                .andExpect(jsonPath("$[1].name").value("deadlift"))
                .andExpect(jsonPath("$[2].name").value("squat"));
    }

    @Test
    @DisplayName("should return 401 when listing exercises without authentication")
    void getExercisesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/exercises")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("should delete authenticated users exercise")
    void deleteExerciseSuccess() throws Exception {
        insertExercise("exercise-1", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));

        mockMvc.perform(delete("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(exercisesById).doesNotContainKey("exercise-1");
    }

    @Test
    @DisplayName("should return not found when exercise does not exist")
    void deleteExerciseMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/exercises/missing")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));
    }

    @Test
    @DisplayName("should return not found when exercise belongs to another user")
    void deleteExerciseOfAnotherUser() throws Exception {
        insertExercise("exercise-1", OTHER_USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));

        mockMvc.perform(delete("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));

        assertThat(exercisesById).containsKey("exercise-1");
    }

    @Test
    @DisplayName("should block deleting exercise that is in use")
    void deleteExerciseInUse() throws Exception {
        insertExercise("exercise-1", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));
        Mockito.when(exerciseReferenceChecker.isExerciseInUse("exercise-1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exercise cannot be deleted because it is used in existing workouts."));

        assertThat(exercisesById).containsKey("exercise-1");
    }

    @Test
    @DisplayName("should update authenticated users exercise")
    void updateExerciseSuccess() throws Exception {
        insertExercise("exercise-1", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));
        UpdateExerciseRequest request = new UpdateExerciseRequest(" Incline Bench Press ", "UPPER CHEST", " Upper chest pressing ");

        mockMvc.perform(put("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("exercise-1"))
                .andExpect(jsonPath("$.name").value("Incline Bench Press"))
                .andExpect(jsonPath("$.category").value("UPPER CHEST"))
                .andExpect(jsonPath("$.description").value("Upper chest pressing"));

        ExerciseDocument updated = exercisesById.get("exercise-1");
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Incline Bench Press");
        assertThat(updated.getNameNormalized()).isEqualTo("incline bench press");
        assertThat(updated.getCategory()).isEqualTo("UPPER CHEST");
        assertThat(updated.getDescription()).isEqualTo("Upper chest pressing");
    }

    @Test
    @DisplayName("should return not found when updated exercise does not exist")
    void updateExerciseMissing() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest pressing");

        mockMvc.perform(put("/api/v1/exercises/missing")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));
    }

    @Test
    @DisplayName("should return not found when updated exercise belongs to another user")
    void updateExerciseOfAnotherUser() throws Exception {
        insertExercise("exercise-1", OTHER_USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest pressing");

        mockMvc.perform(put("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));

        assertThat(exercisesById.get("exercise-1").getName()).isEqualTo("Bench Press");
    }

    @Test
    @DisplayName("should reject duplicate normalized name during update")
    void updateExerciseDuplicateName() throws Exception {
        insertExercise("exercise-1", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));
        insertExercise("exercise-2", USER_ID, "Squat", "LEGS", Instant.parse("2026-04-06T12:00:00Z"));
        UpdateExerciseRequest request = new UpdateExerciseRequest("  SQUAT  ", "CHEST", "Upper chest pressing");

        mockMvc.perform(put("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exercise with this name already exists."));

        assertThat(exercisesById.get("exercise-1").getName()).isEqualTo("Bench Press");
        assertThat(exercisesById.get("exercise-1").getNameNormalized()).isEqualTo("bench press");
    }

    @Test
    @DisplayName("should reject invalid payload when updating exercise")
    void updateExerciseBlankName() throws Exception {
        UpdateExerciseRequest request = new UpdateExerciseRequest(" ", "CHEST", "Upper chest pressing");

        mockMvc.perform(put("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must not be blank"));
    }

    @Test
    @DisplayName("should translate duplicate key race during update into conflict")
    void updateExerciseDuplicateKeyRace() throws Exception {
        insertExercise("exercise-1", USER_ID, "Bench Press", "CHEST", Instant.parse("2026-04-05T12:00:00Z"));
        Mockito.when(exerciseRepository.save(any(ExerciseDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate exercise"));
        UpdateExerciseRequest request = new UpdateExerciseRequest("Incline Bench Press", "CHEST", "Upper chest pressing");

        mockMvc.perform(put("/api/v1/exercises/exercise-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exercise with this name already exists."));
    }

    private UserDocument createUser(String id, String email) {
        return UserDocument.builder()
                .id(id)
                .email(email)
                .provider(UserProvider.LOCAL)
                .roles(List.of("ROLE_USER"))
                .build();
    }

    private String bearerToken(String email) {
        return "Bearer " + jwtService.generateToken(email, List.of("ROLE_USER"));
    }

    private void insertExercise(String id, String userId, String name, String category, Instant createdAt) {
        ExerciseDocument exercise = ExerciseDocument.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .category(category)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        exercisesById.put(id, exercise);
    }

    private Comparator<ExerciseDocument> comparatorFor(Pageable pageable) {
        Sort.Order order = pageable.getSort().stream()
                .findFirst()
                .orElse(Sort.Order.asc("nameNormalized"));

        Comparator<ExerciseDocument> comparator = switch (order.getProperty()) {
            case "createdAt" -> Comparator.comparing(ExerciseDocument::getCreatedAt);
            case "nameNormalized" -> Comparator.comparing(ExerciseDocument::getNameNormalized);
            default -> Comparator.comparing(ExerciseDocument::getId);
        };

        return order.isAscending() ? comparator : comparator.reversed();
    }
}
