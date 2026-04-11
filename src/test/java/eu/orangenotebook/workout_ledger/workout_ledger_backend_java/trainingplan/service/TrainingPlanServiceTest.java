package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.TrainingPlanNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    private static final String USER_EMAIL = "user@email.com";
    private static final String USER_ID = "user-1";

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    private TrainingPlanService trainingPlanService;

    @BeforeEach
    void setUp() {
        trainingPlanService = new TrainingPlanService(
                trainingPlanRepository,
                userRepository,
                new TrainingPlanMapper(),
                exerciseRepository
        );
        lenient().when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user()));
    }

    @Test
    @DisplayName("should create training plan for authenticated user")
    void createTrainingPlanForAuthenticatedUser() {
        CreateTrainingPlanRequest request = createRequest(
                " Push A ",
                " Upper body ",
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1), plannedSetRequest(2))
        );
        when(exerciseRepository.findByIdAndUserId("exercise-1", USER_ID))
                .thenReturn(Optional.of(exercise(" exercise-1 ", " Bench Press ")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument createdTrainingPlan = trainingPlanService.createTrainingPlan(USER_EMAIL, request);

        ArgumentCaptor<TrainingPlanDocument> trainingPlanCaptor = ArgumentCaptor.forClass(TrainingPlanDocument.class);
        verify(trainingPlanRepository).save(trainingPlanCaptor.capture());

        TrainingPlanDocument savedTrainingPlan = trainingPlanCaptor.getValue();
        assertThat(savedTrainingPlan.getUserId()).isEqualTo(USER_ID);
        assertThat(savedTrainingPlan.getName()).isEqualTo("Push A");
        assertThat(savedTrainingPlan.getDescription()).isEqualTo("Upper body");
        assertThat(savedTrainingPlan.isActive()).isTrue();
        assertThat(savedTrainingPlan.getEntries()).hasSize(1);
        assertThat(savedTrainingPlan.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-1");
        assertThat(savedTrainingPlan.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Bench Press");
        assertThat(createdTrainingPlan).isSameAs(savedTrainingPlan);
    }

    @Test
    @DisplayName("should list training plans only for authenticated user")
    void listTrainingPlansUsesAuthenticatedUserId() {
        TrainingPlanDocument newest = trainingPlan("plan-2", "Pull A", Instant.parse("2026-04-08T12:00:00Z"), entry("exercise-2", 1, plannedSet(1)));
        TrainingPlanDocument older = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        when(trainingPlanRepository.findAllByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(newest, older));

        List<TrainingPlanDocument> trainingPlans = trainingPlanService.listTrainingPlans(USER_EMAIL);

        verify(trainingPlanRepository).findAllByUserIdOrderByUpdatedAtDesc(USER_ID);
        assertThat(trainingPlans).containsExactly(newest, older);
    }

    @Test
    @DisplayName("should return training plan only when it belongs to authenticated user")
    void getTrainingPlanSuccess() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));

        TrainingPlanDocument result = trainingPlanService.getTrainingPlan(USER_EMAIL, "plan-1");

        assertThat(result).isSameAs(trainingPlan);
    }

    @Test
    @DisplayName("should throw when training plan belongs to another user")
    void getTrainingPlanForDifferentUser() {
        when(trainingPlanRepository.findByIdAndUserId("foreign-plan", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingPlanService.getTrainingPlan(USER_EMAIL, "foreign-plan"))
                .isInstanceOf(TrainingPlanNotFoundException.class)
                .hasMessage("Training plan not found.");
    }

    @Test
    @DisplayName("should update training plan for authenticated user without changing ownership metadata")
    void updateTrainingPlanSuccess() {
        Instant createdAt = Instant.parse("2026-04-07T12:00:00Z");
        TrainingPlanDocument trainingPlan = TrainingPlanDocument.builder()
                .id("plan-1")
                .userId(USER_ID)
                .name("Push A")
                .description("Before")
                .entries(List.of(entry("exercise-1", 1, plannedSet(1))))
                .active(true)
                .createdAt(createdAt)
                .updatedAt(Instant.parse("2026-04-08T12:00:00Z"))
                .build();
        UpdateTrainingPlanRequest request = updateRequest(
                " Push B ",
                " After ",
                false,
                entryRequest("exercise-2", 1, plannedSetRequest(1), plannedSetRequest(2))
        );
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(exerciseRepository.findByIdAndUserId("exercise-2", USER_ID))
                .thenReturn(Optional.of(exercise("exercise-2", "Incline Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlan(USER_EMAIL, "plan-1", request);

        assertThat(updatedTrainingPlan.getId()).isEqualTo("plan-1");
        assertThat(updatedTrainingPlan.getUserId()).isEqualTo(USER_ID);
        assertThat(updatedTrainingPlan.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedTrainingPlan.getName()).isEqualTo("Push B");
        assertThat(updatedTrainingPlan.getDescription()).isEqualTo("After");
        assertThat(updatedTrainingPlan.isActive()).isFalse();
        assertThat(updatedTrainingPlan.getEntries()).hasSize(1);
        assertThat(updatedTrainingPlan.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-2");
        assertThat(updatedTrainingPlan.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Incline Bench Press");
        verify(trainingPlanRepository).save(trainingPlan);
    }

    @Test
    @DisplayName("should reject training plan create when exercise does not belong to authenticated user")
    void createTrainingPlanRejectsForeignExercise() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                true,
                entryRequest("foreign-exercise", 1, plannedSetRequest(1))
        );
        when(exerciseRepository.findByIdAndUserId("foreign-exercise", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(USER_EMAIL, request))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessage("Exercise not found.");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should delete authenticated users training plan")
    void deleteTrainingPlanSuccess() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));

        trainingPlanService.deleteTrainingPlan(USER_EMAIL, "plan-1");

        verify(trainingPlanRepository).delete(trainingPlan);
    }

    @Test
    @DisplayName("should create training plan when entry orders and set numbers are unique")
    void createTrainingPlanSuccess() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                "plan-1",
                "Push A",
                Instant.parse("2026-04-07T12:00:00Z"),
                entry("exercise-1", 1, plannedSet(1), plannedSet(2)),
                entry("exercise-2", 2, plannedSet(1))
        );
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument savedTrainingPlan = trainingPlanService.createTrainingPlan(trainingPlan);

        ArgumentCaptor<TrainingPlanDocument> trainingPlanCaptor = ArgumentCaptor.forClass(TrainingPlanDocument.class);
        verify(trainingPlanRepository).save(trainingPlanCaptor.capture());
        assertThat(trainingPlanCaptor.getValue()).isSameAs(trainingPlan);
        assertThat(savedTrainingPlan).isSameAs(trainingPlan);
    }

    @Test
    @DisplayName("should reject training plan update when entry order is duplicated")
    void updateTrainingPlanRejectsDuplicatedEntryOrder() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                "plan-1",
                "Push A",
                Instant.parse("2026-04-07T12:00:00Z"),
                entry("exercise-1", 2, plannedSet(1)),
                entry("exercise-2", 2, plannedSet(1))
        );

        assertThatThrownBy(() -> trainingPlanService.updateTrainingPlan(trainingPlan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan contains duplicated entry order: 2");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject training plan create when set number is duplicated within single entry")
    void createTrainingPlanRejectsDuplicatedSetNumberWithinEntry() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                "plan-1",
                "Push A",
                Instant.parse("2026-04-07T12:00:00Z"),
                entry("exercise-1", 1, plannedSet(3), plannedSet(3))
        );

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(trainingPlan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan entry contains duplicated setNumber: 3 for exerciseId=exercise-1");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should allow same set number in different entries")
    void updateTrainingPlanAllowsSameSetNumberInDifferentEntries() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                "plan-1",
                "Push A",
                Instant.parse("2026-04-07T12:00:00Z"),
                entry("exercise-1", 1, plannedSet(1), plannedSet(2)),
                entry("exercise-2", 2, plannedSet(1), plannedSet(2))
        );
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument savedTrainingPlan = trainingPlanService.updateTrainingPlan(trainingPlan);

        verify(trainingPlanRepository).save(trainingPlan);
        assertThat(savedTrainingPlan).isSameAs(trainingPlan);
    }

    @Test
    @DisplayName("should throw when authenticated user cannot be resolved")
    void createTrainingPlanUserMissing() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(
                USER_EMAIL,
                createRequest("Push A", null, true, entryRequest("exercise-1", 1, plannedSetRequest(1)))
        ))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authenticated user not found.");
    }

    private UserDocument user() {
        return UserDocument.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .provider(UserProvider.LOCAL)
                .roles(List.of("ROLE_USER"))
                .build();
    }

    private TrainingPlanDocument trainingPlan(String id, String name, Instant timestamp, TrainingPlanEntry... entries) {
        return TrainingPlanDocument.builder()
                .id(id)
                .userId(USER_ID)
                .name(name)
                .description("Description")
                .entries(List.of(entries))
                .active(true)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }

    private TrainingPlanEntry entry(String exerciseId, Integer order, PlannedSet... plannedSets) {
        return TrainingPlanEntry.builder()
                .exerciseId(exerciseId)
                .exerciseNameSnapshot("Exercise " + exerciseId)
                .order(order)
                .plannedSets(List.of(plannedSets))
                .build();
    }

    private PlannedSet plannedSet(Integer setNumber) {
        return PlannedSet.builder()
                .setNumber(setNumber)
                .reps(10)
                .type(PlannedSetType.NORMAL)
                .build();
    }

    private ExerciseDocument exercise(String id, String name) {
        return ExerciseDocument.builder()
                .id(id)
                .userId(USER_ID)
                .name(name)
                .build();
    }

    private CreateTrainingPlanRequest createRequest(String name,
                                                    String description,
                                                    boolean active,
                                                    TrainingPlanEntryRequest... entries) {
        return new CreateTrainingPlanRequest(name, description, List.of(entries), active);
    }

    private UpdateTrainingPlanRequest updateRequest(String name,
                                                    String description,
                                                    boolean active,
                                                    TrainingPlanEntryRequest... entries) {
        return new UpdateTrainingPlanRequest(name, description, List.of(entries), active);
    }

    private TrainingPlanEntryRequest entryRequest(String exerciseId,
                                                  Integer order,
                                                  PlannedSetRequest... plannedSets) {
        return new TrainingPlanEntryRequest(exerciseId, order, null, List.of(plannedSets));
    }

    private PlannedSetRequest plannedSetRequest(Integer setNumber) {
        return new PlannedSetRequest(setNumber, 10, 60.0, null, null, 90, PlannedSetType.NORMAL);
    }
}
