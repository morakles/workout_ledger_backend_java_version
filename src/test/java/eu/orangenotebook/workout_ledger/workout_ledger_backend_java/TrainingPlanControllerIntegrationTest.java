package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseReferenceChecker;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.CreateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.PlannedSetRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.UpdateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
class TrainingPlanControllerIntegrationTest {

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
    Map<String, TrainingPlanDocument> trainingPlansById;
    Map<String, ExerciseDocument> exercisesByOwnerAndId;

    @BeforeEach
    void setUp() {
        usersByEmail = new ConcurrentHashMap<>();
        trainingPlansById = new ConcurrentHashMap<>();
        exercisesByOwnerAndId = new ConcurrentHashMap<>();

        usersByEmail.put(USER_EMAIL, createUser(USER_ID, USER_EMAIL));
        usersByEmail.put(OTHER_USER_EMAIL, createUser(OTHER_USER_ID, OTHER_USER_EMAIL));
        exercisesByOwnerAndId.put(exerciseKey(USER_ID, "exercise-1"), createExercise("exercise-1", USER_ID, "Bench Press"));
        exercisesByOwnerAndId.put(exerciseKey(USER_ID, "exercise-2"), createExercise("exercise-2", USER_ID, "Incline Bench Press"));
        exercisesByOwnerAndId.put(exerciseKey(OTHER_USER_ID, "foreign-exercise"), createExercise("foreign-exercise", OTHER_USER_ID, "Other User Exercise"));

        Mockito.lenient().when(userRepository.findByEmail(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(usersByEmail.get(invocation.getArgument(0))));

        Mockito.lenient().when(exerciseRepository.findAllByIdInAndUserId(any(), anyString()))
                .thenAnswer(invocation -> {
                    Collection<String> exerciseIds = invocation.getArgument(0);
                    String userId = invocation.getArgument(1);
                    return exerciseIds.stream()
                            .map(exerciseId -> exercisesByOwnerAndId.get(exerciseKey(userId, exerciseId)))
                            .filter(exercise -> exercise != null)
                            .toList();
                });

        Mockito.lenient().when(trainingPlanRepository.save(any(TrainingPlanDocument.class)))
                .thenAnswer(invocation -> {
                    TrainingPlanDocument trainingPlan = invocation.getArgument(0);
                    Instant now = trainingPlan.getId() == null
                            ? Instant.parse("2026-04-09T12:00:00Z")
                            : Instant.parse("2026-04-10T12:00:00Z");
                    TrainingPlanDocument saved = copyTrainingPlan(
                            trainingPlan.getId() != null ? trainingPlan.getId() : UUID.randomUUID().toString(),
                            trainingPlan.getUserId(),
                            trainingPlan.getName(),
                            trainingPlan.getDescription(),
                            trainingPlan.getEntries(),
                            trainingPlan.isActive(),
                            trainingPlan.getCreatedAt() != null ? trainingPlan.getCreatedAt() : now,
                            now
                    );
                    trainingPlansById.put(saved.getId(), saved);
                    return saved;
                });

        Mockito.lenient().when(trainingPlanRepository.findAllByUserIdOrderByUpdatedAtDesc(anyString()))
                .thenAnswer(invocation -> trainingPlansById.values().stream()
                        .filter(trainingPlan -> trainingPlan.getUserId().equals(invocation.getArgument(0)))
                        .sorted(Comparator.comparing(TrainingPlanDocument::getUpdatedAt).reversed()
                                .thenComparing(TrainingPlanDocument::getId))
                        .toList());

        Mockito.lenient().when(trainingPlanRepository.findByIdAndUserId(anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(trainingPlansById.get(invocation.getArgument(0)))
                        .filter(trainingPlan -> trainingPlan.getUserId().equals(invocation.getArgument(1))));

        Mockito.lenient().doAnswer(invocation -> {
            TrainingPlanDocument trainingPlan = invocation.getArgument(0);
            trainingPlansById.remove(trainingPlan.getId());
            return null;
        }).when(trainingPlanRepository).delete(any(TrainingPlanDocument.class));
    }

    @Test
    @DisplayName("should create training plan for authenticated user")
    void createTrainingPlanSuccess() throws Exception {
        CreateTrainingPlanRequest request = createRequest(" Push A ", true);

        mockMvc.perform(post("/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Push A"))
                .andExpect(jsonPath("$.description").value("Upper body"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Bench Press"));

        TrainingPlanDocument saved = trainingPlansById.values().stream()
                .filter(trainingPlan -> trainingPlan.getUserId().equals(USER_ID))
                .findFirst()
                .orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getName()).isEqualTo("Push A");
        assertThat(saved.getDescription()).isEqualTo("Upper body");
        assertThat(saved.getEntries()).hasSize(1);
        assertThat(saved.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-1");
        assertThat(saved.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Bench Press");
    }

    @Test
    @DisplayName("should create training plan without exerciseNameSnapshot in request payload")
    void createTrainingPlanDoesNotRequireExerciseNameSnapshot() throws Exception {
        String requestBody = """
                {
                  "name": "Push A",
                  "description": "Upper body",
                  "entries": [
                    {
                      "exerciseId": "exercise-1",
                      "order": 1,
                      "notes": "Notes",
                      "plannedSets": [
                        {
                          "setNumber": 1,
                          "reps": 10,
                          "weight": 60.0,
                          "restSeconds": 90,
                          "type": "NORMAL"
                        }
                      ]
                    }
                  ],
                  "active": true
                }
                """;

        mockMvc.perform(post("/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Bench Press"));
    }

    @Test
    @DisplayName("should list only authenticated users training plans ordered by updatedAt desc")
    void getTrainingPlansReturnsAuthenticatedUsersPlans() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Pull A", Instant.parse("2026-04-10T12:00:00Z"));
        insertTrainingPlan("plan-3", OTHER_USER_ID, "Legs", Instant.parse("2026-04-09T12:00:00Z"));

        mockMvc.perform(get("/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("plan-2"))
                .andExpect(jsonPath("$[0].name").value("Pull A"))
                .andExpect(jsonPath("$[1].id").value("plan-1"))
                .andExpect(jsonPath("$[1].name").value("Push A"));
    }

    @Test
    @DisplayName("should get training plan by id when it belongs to authenticated user")
    void getTrainingPlanByIdSuccess() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(get("/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push A"))
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Bench Press"));
    }

    @Test
    @DisplayName("should return 404 when getting another users training plan")
    void getTrainingPlanByIdForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(get("/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));
    }

    @Test
    @DisplayName("should update authenticated users training plan")
    void updateTrainingPlanSuccess() throws Exception {
        Instant createdAt = Instant.parse("2026-04-07T12:00:00Z");
        insertTrainingPlan("plan-1", USER_ID, "Push A", createdAt);
        UpdateTrainingPlanRequest request = updateRequest(" Push B ", false);

        mockMvc.perform(put("/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push B"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Incline Bench Press"));

        TrainingPlanDocument updated = trainingPlansById.get("plan-1");
        assertThat(updated).isNotNull();
        assertThat(updated.getUserId()).isEqualTo(USER_ID);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isEqualTo(Instant.parse("2026-04-10T12:00:00Z"));
        assertThat(updated.getName()).isEqualTo("Push B");
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getEntries()).hasSize(1);
        assertThat(updated.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-2");
        assertThat(updated.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Incline Bench Press");
    }

    @Test
    @DisplayName("should return 404 when updating another users training plan")
    void updateTrainingPlanForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanRequest request = updateRequest("Push B", false);

        mockMvc.perform(put("/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));

        assertThat(trainingPlansById.get("plan-1").getName()).isEqualTo("Push A");
    }

    @Test
    @DisplayName("should return 404 when using exercise that belongs to another user")
    void createTrainingPlanWithForeignExercise() throws Exception {
        String requestBody = """
                {
                  "name": "Push A",
                  "description": "Upper body",
                  "entries": [
                    {
                      "exerciseId": "foreign-exercise",
                      "order": 1,
                      "notes": "Notes",
                      "plannedSets": [
                        {
                          "setNumber": 1,
                          "reps": 10,
                          "weight": 60.0,
                          "restSeconds": 90,
                          "type": "NORMAL"
                        }
                      ]
                    }
                  ],
                  "active": true
                }
                """;

        mockMvc.perform(post("/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));
    }

    @Test
    @DisplayName("should delete authenticated users training plan")
    void deleteTrainingPlanSuccess() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(delete("/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(trainingPlansById).doesNotContainKey("plan-1");
    }

    @Test
    @DisplayName("should return 404 when deleting another users training plan")
    void deleteTrainingPlanForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(delete("/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));

        assertThat(trainingPlansById).containsKey("plan-1");
    }

    private UserDocument createUser(String id, String email) {
        return UserDocument.builder()
                .id(id)
                .email(email)
                .provider(UserProvider.LOCAL)
                .roles(List.of("ROLE_USER"))
                .build();
    }

    private ExerciseDocument createExercise(String id, String userId, String name) {
        return ExerciseDocument.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .build();
    }

    private String bearerToken(String email) {
        return "Bearer " + jwtService.generateToken(email, List.of("ROLE_USER"));
    }

    private String exerciseKey(String userId, String exerciseId) {
        return userId + ":" + exerciseId;
    }

    private void insertTrainingPlan(String id, String userId, String name, Instant timestamp) {
        trainingPlansById.put(id, copyTrainingPlan(
                id,
                userId,
                name,
                "Upper body",
                List.of(TrainingPlanEntry.builder()
                        .exerciseId("exercise-1")
                        .exerciseNameSnapshot("Bench Press")
                        .order(1)
                        .plannedSets(List.of(PlannedSet.builder()
                                .setNumber(1)
                                .reps(10)
                                .weight(60.0)
                                .restSeconds(90)
                                .type(PlannedSetType.NORMAL)
                                .build()))
                        .build()),
                true,
                timestamp,
                timestamp
        ));
    }

    private TrainingPlanDocument copyTrainingPlan(String id,
                                                  String userId,
                                                  String name,
                                                  String description,
                                                  List<TrainingPlanEntry> entries,
                                                  boolean active,
                                                  Instant createdAt,
                                                  Instant updatedAt) {
        return TrainingPlanDocument.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .description(description)
                .entries(entries.stream()
                        .map(entry -> TrainingPlanEntry.builder()
                                .exerciseId(entry.getExerciseId())
                                .exerciseNameSnapshot(entry.getExerciseNameSnapshot())
                                .order(entry.getOrder())
                                .notes(entry.getNotes())
                                .plannedSets(entry.getPlannedSets().stream()
                                        .map(set -> PlannedSet.builder()
                                                .setNumber(set.getSetNumber())
                                                .reps(set.getReps())
                                                .weight(set.getWeight())
                                                .durationSeconds(set.getDurationSeconds())
                                                .distanceMeters(set.getDistanceMeters())
                                                .restSeconds(set.getRestSeconds())
                                                .type(set.getType())
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private CreateTrainingPlanRequest createRequest(String name, boolean active) {
        return new CreateTrainingPlanRequest(
                name,
                " Upper body ",
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                active
        );
    }

    private UpdateTrainingPlanRequest updateRequest(String name, boolean active) {
        return new UpdateTrainingPlanRequest(
                name,
                " Upper body ",
                List.of(new TrainingPlanEntryRequest(
                        "exercise-2",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 8, 70.0, null, null, 120, PlannedSetType.NORMAL))
                )),
                active
        );
    }
}
