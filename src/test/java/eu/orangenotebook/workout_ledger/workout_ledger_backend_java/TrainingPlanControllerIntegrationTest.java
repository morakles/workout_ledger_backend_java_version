package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseReferenceChecker;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.CreateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.PlannedSetRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.UpdateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.UpdateTrainingPlanStatusRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository.TrainingPlanRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    MongoTemplate mongoTemplate;

    Map<String, UserDocument> usersByEmail;
    Map<String, TrainingPlanDocument> trainingPlansById;
    Map<String, ExerciseDocument> exercisesByOwnerAndId;
    Map<String, WorkoutDocument> workoutsById;

    @BeforeEach
    void setUp() {
        usersByEmail = new ConcurrentHashMap<>();
        trainingPlansById = new ConcurrentHashMap<>();
        exercisesByOwnerAndId = new ConcurrentHashMap<>();
        workoutsById = new ConcurrentHashMap<>();

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
                            trainingPlan.getType(),
                            trainingPlan.getStatus(),
                            trainingPlan.getPlannedDate(),
                            trainingPlan.getEntries(),
                            trainingPlan.isActive(),
                            trainingPlan.getCreatedAt() != null ? trainingPlan.getCreatedAt() : now,
                            now
                    );
                    trainingPlansById.put(saved.getId(), saved);
                    return saved;
                });

        Mockito.lenient().when(workoutRepository.save(any(WorkoutDocument.class)))
                .thenAnswer(invocation -> {
                    WorkoutDocument workout = invocation.getArgument(0);
                    String id = workout.getId() != null ? workout.getId() : UUID.randomUUID().toString();
                    workout.setId(id);
                    workoutsById.put(id, workout);
                    return workout;
                });

        Mockito.lenient().when(trainingPlanRepository.findAllByUserIdAndFilters(
                        anyString(),
                        Mockito.nullable(TrainingPlanType.class),
                        Mockito.nullable(TrainingPlanStatus.class),
                        Mockito.nullable(LocalDate.class),
                        Mockito.nullable(LocalDate.class)
                ))
                .thenAnswer(invocation -> trainingPlansById.values().stream()
                        .filter(trainingPlan -> trainingPlan.getUserId().equals(invocation.getArgument(0)))
                        .filter(trainingPlan -> matchesType(trainingPlan, invocation.getArgument(1)))
                        .filter(trainingPlan -> matchesStatus(trainingPlan, invocation.getArgument(2)))
                        .filter(trainingPlan -> matchesDateRange(trainingPlan, invocation.getArgument(3), invocation.getArgument(4)))
                        .sorted(Comparator.comparing(
                                        TrainingPlanControllerIntegrationTest::plannedDateSortValue)
                                .thenComparing(TrainingPlanDocument::getId))
                        .toList());

        Mockito.lenient().when(trainingPlanRepository.findByIdAndUserId(anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(trainingPlansById.get(invocation.getArgument(0)))
                        .filter(trainingPlan -> trainingPlan.getUserId().equals(invocation.getArgument(1))));

        Mockito.lenient().when(trainingPlanRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(trainingPlansById.get(invocation.getArgument(0))));

        Mockito.lenient().doAnswer(invocation -> {
            TrainingPlanDocument trainingPlan = invocation.getArgument(0);
            trainingPlansById.remove(trainingPlan.getId());
            return null;
        }).when(trainingPlanRepository).delete(any(TrainingPlanDocument.class));
    }

    @Test
    @DisplayName("should create template training plan for authenticated user")
    void createTemplateTrainingPlanSuccess() throws Exception {
        CreateTrainingPlanRequest request = createTemplateRequest(" Push A ", true);

        mockMvc.perform(post("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Push A"))
                .andExpect(jsonPath("$.description").value("Upper body"))
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist())
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
        assertThat(saved.getType()).isEqualTo(TrainingPlanType.TEMPLATE);
        assertThat(saved.getStatus()).isNull();
        assertThat(saved.getPlannedDate()).isNull();
        assertThat(saved.getEntries()).hasSize(1);
        assertThat(saved.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-1");
        assertThat(saved.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Bench Press");
    }

    @Test
    @DisplayName("should create planned workout for authenticated user")
    void createPlannedWorkoutSuccess() throws Exception {
        CreateTrainingPlanRequest request = createPlannedWorkoutRequest(" Pull A ", LocalDate.parse("2026-04-15"), true);

        mockMvc.perform(post("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pull A"))
                .andExpect(jsonPath("$.type").value("PLANNED_WORKOUT"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.plannedDate").value("2026-04-15"));

        TrainingPlanDocument saved = trainingPlansById.values().stream()
                .filter(trainingPlan -> trainingPlan.getUserId().equals(USER_ID))
                .findFirst()
                .orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getType()).isEqualTo(TrainingPlanType.PLANNED_WORKOUT);
        assertThat(saved.getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
        assertThat(saved.getPlannedDate()).isEqualTo(LocalDate.parse("2026-04-15"));
    }

    @Test
    @DisplayName("should create training plan without exerciseNameSnapshot in request payload")
    void createTrainingPlanDoesNotRequireExerciseNameSnapshot() throws Exception {
        String requestBody = """
                {
                  "name": "Push A",
                  "description": "Upper body",
                  "type": "TEMPLATE",
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

        mockMvc.perform(post("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Bench Press"));
    }

    @Test
    @DisplayName("should return full training plan response contract by default")
    void getTrainingPlansReturnsAuthenticatedUsersPlans() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Pull A", Instant.parse("2026-04-10T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Legs", Instant.parse("2026-04-09T12:00:00Z"));
        insertTrainingPlan("plan-4", OTHER_USER_ID, "Other User Plan", Instant.parse("2026-04-11T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains("plan-1", "plan-2", "plan-3")))
                .andExpect(jsonPath("$[*].type").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("TEMPLATE"))))
                .andExpect(jsonPath("$[0].description").value("Upper body"))
                .andExpect(jsonPath("$[0].entries[0].exerciseId").value("exercise-1"))
                .andExpect(jsonPath("$[0].entries[0].exerciseNameSnapshot").value("Bench Press"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].createdAt").value("2026-04-08T12:00:00Z"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-04-08T12:00:00Z"))
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    @DisplayName("should expose lightweight training plan summaries explicitly")
    void getTrainingPlanSummariesReturnsLightweightRepresentation() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Workout A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Template A", Instant.parse("2026-04-09T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans/summaries")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains("plan-1", "plan-2")))
                .andExpect(jsonPath("$[0].name").value("Workout A"))
                .andExpect(jsonPath("$[0].type").value("PLANNED_WORKOUT"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"))
                .andExpect(jsonPath("$[0].plannedDate").value("2026-04-12"))
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].entries").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
    }

    @Test
    @DisplayName("should filter training plans by from date")
    void getTrainingPlansFiltersByFrom() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-10"),
                Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Pull A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-15"),
                Instant.parse("2026-04-10T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Template A", Instant.parse("2026-04-09T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("from", "2026-04-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("plan-2"))
                .andExpect(jsonPath("$[0].plannedDate").value("2026-04-15"));
    }

    @Test
    @DisplayName("should filter training plans by to date")
    void getTrainingPlansFiltersByTo() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-10"),
                Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Pull A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-15"),
                Instant.parse("2026-04-10T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Template A", Instant.parse("2026-04-09T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("to", "2026-04-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("plan-1"))
                .andExpect(jsonPath("$[0].plannedDate").value("2026-04-10"));
    }

    @Test
    @DisplayName("should filter training plans by date range")
    void getTrainingPlansFiltersByFromAndTo() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-10"),
                Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Pull A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                Instant.parse("2026-04-09T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Legs A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-15"),
                Instant.parse("2026-04-10T12:00:00Z"));
        insertTrainingPlan("plan-4", USER_ID, "Template A", Instant.parse("2026-04-11T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("from", "2026-04-11")
                        .param("to", "2026-04-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("plan-2"))
                .andExpect(jsonPath("$[0].plannedDate").value("2026-04-12"));
    }

    @Test
    @DisplayName("should filter training plans by type and include legacy templates without type")
    void getTrainingPlansFiltersByType() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Template A", Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlanWithRawType("plan-2", USER_ID, "Legacy Template", null, null, Instant.parse("2026-04-09T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Workout A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-15"),
                Instant.parse("2026-04-10T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("type", "TEMPLATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains("plan-1", "plan-2")))
                .andExpect(jsonPath("$[*].type").value(org.hamcrest.Matchers.contains("TEMPLATE", "TEMPLATE")));
    }

    @Test
    @DisplayName("should filter training plans by status for planned workouts only")
    void getTrainingPlansFiltersByStatus() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Workout A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-10"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Template A", Instant.parse("2026-04-09T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Workout Done", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.DONE, Instant.parse("2026-04-10T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("plan-1"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    @DisplayName("should sort training plans by plannedDate ascending")
    void getTrainingPlansSortsByPlannedDate() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Workout C", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-15"),
                Instant.parse("2026-04-10T12:00:00Z"));
        insertTrainingPlan("plan-2", USER_ID, "Workout A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-10"),
                Instant.parse("2026-04-08T12:00:00Z"));
        insertTrainingPlan("plan-3", USER_ID, "Workout B", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                Instant.parse("2026-04-09T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("type", "PLANNED_WORKOUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains("plan-2", "plan-3", "plan-1")))
                .andExpect(jsonPath("$[*].plannedDate").value(org.hamcrest.Matchers.contains(
                        "2026-04-10",
                        "2026-04-12",
                        "2026-04-15"
                )));
    }

    @Test
    @DisplayName("should isolate training plans between users")
    void getTrainingPlansIsolatesUsers() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Other User Workout", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-15"),
                Instant.parse("2026-04-10T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("should return 400 when from is after to")
    void getTrainingPlansRejectsInvalidRange() throws Exception {
        mockMvc.perform(get("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .param("from", "2026-04-20")
                        .param("to", "2026-04-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Query param 'from' must be before or equal to 'to'."));
    }

    @Test
    @DisplayName("should get training plan by id when it belongs to authenticated user")
    void getTrainingPlanByIdSuccess() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push A"))
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Bench Press"));
    }

    @Test
    @DisplayName("should return 404 when getting another users training plan")
    void getTrainingPlanByIdForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(get("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));
    }

    @Test
    @DisplayName("should update template training plan")
    void updateTemplateTrainingPlanSuccess() throws Exception {
        Instant createdAt = Instant.parse("2026-04-07T12:00:00Z");
        insertTrainingPlan("plan-1", USER_ID, "Push A", createdAt);
        UpdateTrainingPlanRequest request = updateTemplateRequest(" Push B ", false);

        mockMvc.perform(put("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push B"))
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.entries[0].exerciseNameSnapshot").value("Incline Bench Press"));

        TrainingPlanDocument updated = trainingPlansById.get("plan-1");
        assertThat(updated).isNotNull();
        assertThat(updated.getUserId()).isEqualTo(USER_ID);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isEqualTo(Instant.parse("2026-04-10T12:00:00Z"));
        assertThat(updated.getName()).isEqualTo("Push B");
        assertThat(updated.getType()).isEqualTo(TrainingPlanType.TEMPLATE);
        assertThat(updated.getStatus()).isNull();
        assertThat(updated.getPlannedDate()).isNull();
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getEntries()).hasSize(1);
        assertThat(updated.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-2");
        assertThat(updated.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Incline Bench Press");
    }

    @Test
    @DisplayName("should update planned workout")
    void updatePlannedWorkoutSuccess() throws Exception {
        Instant createdAt = Instant.parse("2026-04-07T12:00:00Z");
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"), createdAt);
        UpdateTrainingPlanRequest request = updatePlannedWorkoutRequest(" Push B ", LocalDate.parse("2026-04-20"), false);

        mockMvc.perform(put("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push B"))
                .andExpect(jsonPath("$.type").value("PLANNED_WORKOUT"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.plannedDate").value("2026-04-20"))
                .andExpect(jsonPath("$.active").value(false));

        TrainingPlanDocument updated = trainingPlansById.get("plan-1");
        assertThat(updated.getType()).isEqualTo(TrainingPlanType.PLANNED_WORKOUT);
        assertThat(updated.getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
        assertThat(updated.getPlannedDate()).isEqualTo(LocalDate.parse("2026-04-20"));
    }

    @Test
    @DisplayName("should switch template to planned workout")
    void updateTrainingPlanSwitchesTemplateToPlannedWorkout() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-07T12:00:00Z"));
        UpdateTrainingPlanRequest request = updatePlannedWorkoutRequest(" Push B ", LocalDate.parse("2026-04-21"), true);

        mockMvc.perform(put("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PLANNED_WORKOUT"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.plannedDate").value("2026-04-21"));

        TrainingPlanDocument updated = trainingPlansById.get("plan-1");
        assertThat(updated.getType()).isEqualTo(TrainingPlanType.PLANNED_WORKOUT);
        assertThat(updated.getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
        assertThat(updated.getPlannedDate()).isEqualTo(LocalDate.parse("2026-04-21"));
    }

    @Test
    @DisplayName("should switch planned workout to template and clear planned date")
    void updateTrainingPlanSwitchesPlannedWorkoutToTemplate() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                Instant.parse("2026-04-07T12:00:00Z"));
        UpdateTrainingPlanRequest request = updateTemplateRequest(" Push B ", true);

        mockMvc.perform(put("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.plannedDate").doesNotExist());

        TrainingPlanDocument updated = trainingPlansById.get("plan-1");
        assertThat(updated.getType()).isEqualTo(TrainingPlanType.TEMPLATE);
        assertThat(updated.getStatus()).isNull();
        assertThat(updated.getPlannedDate()).isNull();
    }

    @Test
    @DisplayName("should update training plan status from planned to done")
    void updateTrainingPlanStatusFromPlannedToDone() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanStatusRequest request = new UpdateTrainingPlanStatusRequest(TrainingPlanStatus.DONE);

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.status").value("DONE"));

        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.DONE);
    }

    @Test
    @DisplayName("should update training plan status from planned to skipped")
    void updateTrainingPlanStatusFromPlannedToSkipped() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanStatusRequest request = new UpdateTrainingPlanStatusRequest(TrainingPlanStatus.SKIPPED);

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.status").value("SKIPPED"));

        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.SKIPPED);
    }

    @Test
    @DisplayName("should reject status transition from done to skipped")
    void updateTrainingPlanStatusFromDoneToSkippedFails() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.DONE, Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanStatusRequest request = new UpdateTrainingPlanStatusRequest(TrainingPlanStatus.SKIPPED);

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Training plan status cannot transition from DONE to SKIPPED."));

        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.DONE);
    }

    @Test
    @DisplayName("should return 400 when updating status of template")
    void updateTrainingPlanStatusForTemplateFails() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanStatusRequest request = new UpdateTrainingPlanStatusRequest(TrainingPlanStatus.DONE);

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Training plan status can be updated only for PLANNED_WORKOUT."));

        assertThat(trainingPlansById.get("plan-1").getStatus()).isNull();
    }

    @Test
    @DisplayName("should return 404 when updating status of another users training plan")
    void updateTrainingPlanStatusForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanStatusRequest request = new UpdateTrainingPlanStatusRequest(TrainingPlanStatus.DONE);

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));

        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
    }

    @Test
    @DisplayName("should reject null training plan status update request")
    void updateTrainingPlanStatusRejectsNullStatus() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must not be null"));
    }

    @Test
    @DisplayName("should reject invalid training plan status enum value")
    void updateTrainingPlanStatusRejectsInvalidEnumValue() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(patch("/api/v1/training-plans/plan-1/status")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body."));
    }

    @Test
    @DisplayName("should start planned workout as workout for authenticated user")
    void startTrainingPlanCreatesWorkoutAndMarksPlanDone() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(post("/api/v1/training-plans/plan-1/start")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workoutId").exists())
                .andExpect(jsonPath("$.trainingPlanId").value("plan-1"));

        assertThat(workoutsById).hasSize(1);
        WorkoutDocument savedWorkout = workoutsById.values().iterator().next();
        assertThat(savedWorkout.getUserId()).isEqualTo(USER_ID);
        assertThat(savedWorkout.getName()).isEqualTo("Push A");
        assertThat(savedWorkout.getWorkoutDate()).isNotNull();
        assertThat(savedWorkout.getEntries()).hasSize(1);
        assertThat(savedWorkout.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-1");
        assertThat(savedWorkout.getEntries().getFirst().getSets()).hasSize(1);
        assertThat(savedWorkout.getEntries().getFirst().getSets().getFirst().getReps()).isEqualTo(10);
        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.DONE);
    }

    @Test
    @DisplayName("should return 404 when starting another users training plan")
    void startTrainingPlanForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.PLANNED, Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(post("/api/v1/training-plans/plan-1/start")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));

        assertThat(workoutsById).isEmpty();
        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
    }

    @Test
    @DisplayName("should return 400 when starting training plan without exercises")
    void startTrainingPlanWithoutExercisesFails() throws Exception {
        trainingPlansById.put("plan-1", copyTrainingPlan(
                "plan-1",
                USER_ID,
                "Push A",
                "Upper body",
                TrainingPlanType.PLANNED_WORKOUT,
                TrainingPlanStatus.PLANNED,
                LocalDate.parse("2026-04-12"),
                List.of(),
                true,
                Instant.parse("2026-04-08T12:00:00Z"),
                Instant.parse("2026-04-08T12:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/training-plans/plan-1/start")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Training plan has no exercises."));

        assertThat(workoutsById).isEmpty();
        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
    }

    @Test
    @DisplayName("should return 409 when starting already done planned workout")
    void startTrainingPlanAlreadyDoneFails() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                TrainingPlanStatus.DONE, Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(post("/api/v1/training-plans/plan-1/start")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Training plan cannot be started from status DONE."));

        assertThat(workoutsById).isEmpty();
        assertThat(trainingPlansById.get("plan-1").getStatus()).isEqualTo(TrainingPlanStatus.DONE);
    }

    @Test
    @DisplayName("should return 404 when updating another users training plan")
    void updateTrainingPlanForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));
        UpdateTrainingPlanRequest request = updateTemplateRequest("Push B", false);

        mockMvc.perform(put("/api/v1/training-plans/plan-1")
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
                  "type": "TEMPLATE",
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

        mockMvc.perform(post("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exercise not found."));
    }

    @Test
    @DisplayName("should return 400 when creating template with planned date")
    void createTemplateWithPlannedDateRejectsRequest() throws Exception {
        CreateTrainingPlanRequest request = new CreateTrainingPlanRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.TEMPLATE,
                LocalDate.parse("2026-04-12"),
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                true
        );

        mockMvc.perform(post("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Training plan of type TEMPLATE must not define plannedDate."));
    }

    @Test
    @DisplayName("should return 400 when creating planned workout without planned date")
    void createPlannedWorkoutWithoutPlannedDateRejectsRequest() throws Exception {
        CreateTrainingPlanRequest request = new CreateTrainingPlanRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.PLANNED_WORKOUT,
                null,
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                true
        );

        mockMvc.perform(post("/api/v1/training-plans")
                        .header("Authorization", bearerToken(USER_EMAIL))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Training plan of type PLANNED_WORKOUT requires plannedDate."));
    }

    @Test
    @DisplayName("should delete authenticated users training plan")
    void deleteTrainingPlanSuccess() throws Exception {
        insertTrainingPlan("plan-1", USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(delete("/api/v1/training-plans/plan-1")
                        .header("Authorization", bearerToken(USER_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(trainingPlansById).doesNotContainKey("plan-1");
    }

    @Test
    @DisplayName("should return 404 when deleting another users training plan")
    void deleteTrainingPlanForDifferentUser() throws Exception {
        insertTrainingPlan("plan-1", OTHER_USER_ID, "Push A", Instant.parse("2026-04-08T12:00:00Z"));

        mockMvc.perform(delete("/api/v1/training-plans/plan-1")
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
        insertTrainingPlan(id, userId, name, TrainingPlanType.TEMPLATE, null, null, timestamp);
    }

    private void insertTrainingPlan(String id,
                                    String userId,
                                    String name,
                                    TrainingPlanType type,
                                    LocalDate plannedDate,
                                    Instant timestamp) {
        insertTrainingPlan(
                id,
                userId,
                name,
                type,
                plannedDate,
                type == TrainingPlanType.PLANNED_WORKOUT ? TrainingPlanStatus.PLANNED : null,
                timestamp
        );
    }

    private void insertTrainingPlan(String id,
                                    String userId,
                                    String name,
                                    TrainingPlanType type,
                                    LocalDate plannedDate,
                                    TrainingPlanStatus status,
                                    Instant timestamp) {
        trainingPlansById.put(id, copyTrainingPlan(
                id,
                userId,
                name,
                "Upper body",
                type,
                status,
                plannedDate,
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

    private void insertTrainingPlanWithRawType(String id,
                                               String userId,
                                               String name,
                                               TrainingPlanType type,
                                               LocalDate plannedDate,
                                               Instant timestamp) {
        insertTrainingPlanWithRawStatus(
                id,
                userId,
                name,
                type,
                plannedDate,
                type == TrainingPlanType.PLANNED_WORKOUT ? TrainingPlanStatus.PLANNED : null,
                timestamp
        );
    }

    private void insertTrainingPlanWithRawStatus(String id,
                                                 String userId,
                                                 String name,
                                                 TrainingPlanType type,
                                                 LocalDate plannedDate,
                                                 TrainingPlanStatus status,
                                                 Instant timestamp) {
        trainingPlansById.put(id, copyTrainingPlan(
                id,
                userId,
                name,
                "Upper body",
                type,
                status,
                plannedDate,
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
                                                  TrainingPlanType type,
                                                  TrainingPlanStatus status,
                                                  LocalDate plannedDate,
                                                  List<TrainingPlanEntry> entries,
                                                  boolean active,
                                                  Instant createdAt,
                                                  Instant updatedAt) {
        return TrainingPlanDocument.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .description(description)
                .type(type)
                .status(status)
                .plannedDate(plannedDate)
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

    private boolean matchesType(TrainingPlanDocument trainingPlan, TrainingPlanType type) {
        if (type == null) {
            return true;
        }

        TrainingPlanType effectiveType = trainingPlan.getType() != null ? trainingPlan.getType() : TrainingPlanType.TEMPLATE;
        return effectiveType == type;
    }

    private boolean matchesStatus(TrainingPlanDocument trainingPlan, TrainingPlanStatus status) {
        if (status == null) {
            return true;
        }
        return trainingPlan.getStatus() == status;
    }

    private boolean matchesDateRange(TrainingPlanDocument trainingPlan, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (trainingPlan.getPlannedDate() == null) {
            return false;
        }
        if (from != null && trainingPlan.getPlannedDate().isBefore(from)) {
            return false;
        }
        return to == null || !trainingPlan.getPlannedDate().isAfter(to);
    }

    private static LocalDate plannedDateSortValue(TrainingPlanDocument trainingPlan) {
        return trainingPlan.getPlannedDate() != null
                ? trainingPlan.getPlannedDate()
                : LocalDate.of(9999, 12, 31);
    }

    private CreateTrainingPlanRequest createTemplateRequest(String name, boolean active) {
        return new CreateTrainingPlanRequest(
                name,
                " Upper body ",
                TrainingPlanType.TEMPLATE,
                null,
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                active
        );
    }

    private CreateTrainingPlanRequest createPlannedWorkoutRequest(String name, LocalDate plannedDate, boolean active) {
        return new CreateTrainingPlanRequest(
                name,
                " Upper body ",
                TrainingPlanType.PLANNED_WORKOUT,
                plannedDate,
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                active
        );
    }

    private UpdateTrainingPlanRequest updateTemplateRequest(String name, boolean active) {
        return new UpdateTrainingPlanRequest(
                name,
                " Upper body ",
                TrainingPlanType.TEMPLATE,
                null,
                List.of(new TrainingPlanEntryRequest(
                        "exercise-2",
                        1,
                        " Notes ",
                        List.of(new PlannedSetRequest(1, 8, 70.0, null, null, 120, PlannedSetType.NORMAL))
                )),
                active
        );
    }

    private UpdateTrainingPlanRequest updatePlannedWorkoutRequest(String name, LocalDate plannedDate, boolean active) {
        return new UpdateTrainingPlanRequest(
                name,
                " Upper body ",
                TrainingPlanType.PLANNED_WORKOUT,
                plannedDate,
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
