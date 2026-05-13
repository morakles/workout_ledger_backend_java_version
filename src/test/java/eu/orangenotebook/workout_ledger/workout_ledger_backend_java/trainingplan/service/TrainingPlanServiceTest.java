package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.InvalidTrainingPlanStatusTransitionException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.TrainingPlanNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.TrainingPlanStatusNotAllowedException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.CreateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.PlannedSetRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanListItemResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.UpdateTrainingPlanRequest;
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
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private WorkoutRepository workoutRepository;

    private TrainingPlanService trainingPlanService;

    @BeforeEach
    void setUp() {
        trainingPlanService = new TrainingPlanService(
                trainingPlanRepository,
                userRepository,
                new TrainingPlanMapper(),
                exerciseRepository,
                workoutRepository
        );
        lenient().when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user()));
    }

    @Test
    @DisplayName("should create training plan for authenticated user")
    void createTrainingPlanForAuthenticatedUser() {
        CreateTrainingPlanRequest request = createRequest(
                " Push A ",
                " Upper body ",
                TrainingPlanType.TEMPLATE,
                null,
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1), plannedSetRequest(2)),
                entryRequest(" exercise-2 ", 2, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1", "exercise-2")), eq(USER_ID)))
                .thenReturn(List.of(
                        exercise(" exercise-1 ", " Bench Press "),
                        exercise("exercise-2", " Incline Bench Press ")
                ));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument createdTrainingPlan = trainingPlanService.createTrainingPlan(USER_EMAIL, request);

        ArgumentCaptor<TrainingPlanDocument> trainingPlanCaptor = ArgumentCaptor.forClass(TrainingPlanDocument.class);
        verify(trainingPlanRepository).save(trainingPlanCaptor.capture());

        TrainingPlanDocument savedTrainingPlan = trainingPlanCaptor.getValue();
        assertThat(savedTrainingPlan.getUserId()).isEqualTo(USER_ID);
        assertThat(savedTrainingPlan.getName()).isEqualTo("Push A");
        assertThat(savedTrainingPlan.getDescription()).isEqualTo("Upper body");
        assertThat(savedTrainingPlan.getType()).isEqualTo(TrainingPlanType.TEMPLATE);
        assertThat(savedTrainingPlan.getStatus()).isNull();
        assertThat(savedTrainingPlan.getPlannedDate()).isNull();
        assertThat(savedTrainingPlan.isActive()).isTrue();
        assertThat(savedTrainingPlan.getEntries()).hasSize(2);
        assertThat(savedTrainingPlan.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-1");
        assertThat(savedTrainingPlan.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Bench Press");
        assertThat(savedTrainingPlan.getEntries().get(1).getExerciseId()).isEqualTo("exercise-2");
        assertThat(savedTrainingPlan.getEntries().get(1).getExerciseNameSnapshot()).isEqualTo("Incline Bench Press");
        assertThat(createdTrainingPlan).isSameAs(savedTrainingPlan);
        verify(exerciseRepository).findAllByIdInAndUserId(Set.of("exercise-1", "exercise-2"), USER_ID);
        verify(exerciseRepository, never()).findByIdAndUserId(anyString(), anyString());
    }

    @Test
    @DisplayName("should list training plans only for authenticated user")
    void listTrainingPlansUsesAuthenticatedUserId() {
        TrainingPlanDocument newest = trainingPlan("plan-2", "Pull A", Instant.parse("2026-04-08T12:00:00Z"), entry("exercise-2", 1, plannedSet(1)));
        TrainingPlanDocument older = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        when(trainingPlanRepository.findAllByUserIdAndFilters(USER_ID, null, null, null, null)).thenReturn(List.of(newest, older));

        List<TrainingPlanDocument> trainingPlans = trainingPlanService.listTrainingPlans(USER_EMAIL);

        verify(trainingPlanRepository).findAllByUserIdAndFilters(USER_ID, null, null, null, null);
        assertThat(trainingPlans).containsExactly(newest, older);
    }

    @Test
    @DisplayName("should return full list responses with backward compatible fields")
    void listTrainingPlanResponsesMapsFullContract() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        when(trainingPlanRepository.findAllByUserIdAndFilters(USER_ID, null, null, null, null)).thenReturn(List.of(trainingPlan));

        List<TrainingPlanResponse> responses = trainingPlanService.listTrainingPlanResponses(
                USER_EMAIL,
                TrainingPlanListQuery.unfiltered()
        );

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo("plan-1");
            assertThat(response.description()).isEqualTo("Description");
            assertThat(response.entries()).hasSize(1);
            assertThat(response.active()).isTrue();
            assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-04-07T12:00:00Z"));
            assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-04-07T12:00:00Z"));
        });
    }

    @Test
    @DisplayName("should return summary list items without changing query behavior")
    void listTrainingPlanSummariesMapsSummaryProjection() {
        TrainingPlanDocument plannedWorkout = trainingPlan("plan-2", "Pull A", Instant.parse("2026-04-08T12:00:00Z"), entry("exercise-2", 1, plannedSet(1)));
        plannedWorkout.setType(TrainingPlanType.PLANNED_WORKOUT);
        plannedWorkout.setStatus(TrainingPlanStatus.PLANNED);
        plannedWorkout.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findAllByUserIdAndFilters(USER_ID, TrainingPlanType.PLANNED_WORKOUT, null, null, null))
                .thenReturn(List.of(plannedWorkout));

        List<TrainingPlanListItemResponse> responses = trainingPlanService.listTrainingPlanSummaries(
                USER_EMAIL,
                new TrainingPlanListQuery(null, null, TrainingPlanType.PLANNED_WORKOUT, null)
        );

        assertThat(responses).containsExactly(new TrainingPlanListItemResponse(
                "plan-2",
                "Pull A",
                TrainingPlanType.PLANNED_WORKOUT,
                TrainingPlanStatus.PLANNED,
                LocalDate.parse("2026-04-12")
        ));
    }

    @Test
    @DisplayName("should create planned workout for authenticated user")
    void createPlannedWorkoutForAuthenticatedUser() {
        LocalDate plannedDate = LocalDate.parse("2026-04-15");
        CreateTrainingPlanRequest request = createRequest(
                " Pull A ",
                " Upper body ",
                TrainingPlanType.PLANNED_WORKOUT,
                plannedDate,
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-1", "Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument createdTrainingPlan = trainingPlanService.createTrainingPlan(USER_EMAIL, request);

        assertThat(createdTrainingPlan.getType()).isEqualTo(TrainingPlanType.PLANNED_WORKOUT);
        assertThat(createdTrainingPlan.getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
        assertThat(createdTrainingPlan.getPlannedDate()).isEqualTo(plannedDate);
    }

    @Test
    @DisplayName("should pass type and date filters to repository")
    void listTrainingPlansPassesFiltersToRepository() {
        LocalDate from = LocalDate.parse("2026-04-10");
        LocalDate to = LocalDate.parse("2026-04-20");
        TrainingPlanDocument plannedWorkout = trainingPlan(
                "plan-2",
                "Pull A",
                Instant.parse("2026-04-08T12:00:00Z"),
                entry("exercise-2", 1, plannedSet(1))
        );
        plannedWorkout.setType(TrainingPlanType.PLANNED_WORKOUT);
        plannedWorkout.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findAllByUserIdAndFilters(USER_ID, TrainingPlanType.PLANNED_WORKOUT, null, from, to))
                .thenReturn(List.of(plannedWorkout));

        List<TrainingPlanDocument> trainingPlans = trainingPlanService.listTrainingPlans(
                USER_EMAIL,
                from,
                to,
                TrainingPlanType.PLANNED_WORKOUT,
                null
        );

        verify(trainingPlanRepository).findAllByUserIdAndFilters(USER_ID, TrainingPlanType.PLANNED_WORKOUT, null, from, to);
        assertThat(trainingPlans).containsExactly(plannedWorkout);
    }

    @Test
    @DisplayName("should reject invalid date range for list endpoint")
    void listTrainingPlansRejectsInvalidRange() {
        assertThatThrownBy(() -> trainingPlanService.listTrainingPlans(
                USER_EMAIL,
                LocalDate.parse("2026-04-20"),
                LocalDate.parse("2026-04-10"),
                TrainingPlanType.PLANNED_WORKOUT,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query param 'from' must be before or equal to 'to'.");

        verify(trainingPlanRepository, never()).findAllByUserIdAndFilters(anyString(), any(), any(), any(), any());
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
                TrainingPlanType.TEMPLATE,
                null,
                false,
                entryRequest("exercise-2", 1, plannedSetRequest(1), plannedSetRequest(2))
        );
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-2")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-2", "Incline Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlan(USER_EMAIL, "plan-1", request);

        assertThat(updatedTrainingPlan.getId()).isEqualTo("plan-1");
        assertThat(updatedTrainingPlan.getUserId()).isEqualTo(USER_ID);
        assertThat(updatedTrainingPlan.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedTrainingPlan.getName()).isEqualTo("Push B");
        assertThat(updatedTrainingPlan.getDescription()).isEqualTo("After");
        assertThat(updatedTrainingPlan.getType()).isEqualTo(TrainingPlanType.TEMPLATE);
        assertThat(updatedTrainingPlan.getStatus()).isNull();
        assertThat(updatedTrainingPlan.getPlannedDate()).isNull();
        assertThat(updatedTrainingPlan.isActive()).isFalse();
        assertThat(updatedTrainingPlan.getEntries()).hasSize(1);
        assertThat(updatedTrainingPlan.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-2");
        assertThat(updatedTrainingPlan.getEntries().getFirst().getExerciseNameSnapshot()).isEqualTo("Incline Bench Press");
        verify(trainingPlanRepository).save(trainingPlan);
        verify(exerciseRepository).findAllByIdInAndUserId(Set.of("exercise-2"), USER_ID);
        verify(exerciseRepository, never()).findByIdAndUserId(anyString(), anyString());
    }

    @Test
    @DisplayName("should update planned workout for authenticated user")
    void updatePlannedWorkoutSuccess() {
        Instant createdAt = Instant.parse("2026-04-07T12:00:00Z");
        LocalDate plannedDate = LocalDate.parse("2026-04-18");
        TrainingPlanDocument trainingPlan = TrainingPlanDocument.builder()
                .id("plan-1")
                .userId(USER_ID)
                .name("Push A")
                .description("Before")
                .type(TrainingPlanType.PLANNED_WORKOUT)
                .plannedDate(LocalDate.parse("2026-04-12"))
                .entries(List.of(entry("exercise-1", 1, plannedSet(1))))
                .active(true)
                .createdAt(createdAt)
                .updatedAt(Instant.parse("2026-04-08T12:00:00Z"))
                .build();
        UpdateTrainingPlanRequest request = updateRequest(
                " Push B ",
                " After ",
                TrainingPlanType.PLANNED_WORKOUT,
                plannedDate,
                false,
                entryRequest("exercise-2", 1, plannedSetRequest(1))
        );
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-2")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-2", "Incline Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlan(USER_EMAIL, "plan-1", request);

        assertThat(updatedTrainingPlan.getType()).isEqualTo(TrainingPlanType.PLANNED_WORKOUT);
        assertThat(updatedTrainingPlan.getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
        assertThat(updatedTrainingPlan.getPlannedDate()).isEqualTo(plannedDate);
    }

    @Test
    @DisplayName("should switch template to planned workout")
    void updateTrainingPlanSwitchesTemplateToPlannedWorkout() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        LocalDate plannedDate = LocalDate.parse("2026-04-21");
        UpdateTrainingPlanRequest request = updateRequest(
                " Push B ",
                " After ",
                TrainingPlanType.PLANNED_WORKOUT,
                plannedDate,
                true,
                entryRequest("exercise-2", 1, plannedSetRequest(1))
        );
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-2")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-2", "Incline Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlan(USER_EMAIL, "plan-1", request);

        assertThat(updatedTrainingPlan.getType()).isEqualTo(TrainingPlanType.PLANNED_WORKOUT);
        assertThat(updatedTrainingPlan.getStatus()).isEqualTo(TrainingPlanStatus.PLANNED);
        assertThat(updatedTrainingPlan.getPlannedDate()).isEqualTo(plannedDate);
    }

    @Test
    @DisplayName("should switch planned workout to template")
    void updateTrainingPlanSwitchesPlannedWorkoutToTemplate() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        trainingPlan.setType(TrainingPlanType.PLANNED_WORKOUT);
        trainingPlan.setStatus(TrainingPlanStatus.DONE);
        trainingPlan.setPlannedDate(LocalDate.parse("2026-04-12"));
        UpdateTrainingPlanRequest request = updateRequest(
                " Push B ",
                " After ",
                TrainingPlanType.TEMPLATE,
                null,
                true,
                entryRequest("exercise-2", 1, plannedSetRequest(1))
        );
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-2")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-2", "Incline Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlan(USER_EMAIL, "plan-1", request);

        assertThat(updatedTrainingPlan.getType()).isEqualTo(TrainingPlanType.TEMPLATE);
        assertThat(updatedTrainingPlan.getStatus()).isNull();
        assertThat(updatedTrainingPlan.getPlannedDate()).isNull();
    }

    @Test
    @DisplayName("should update training plan status from planned to done")
    void updateTrainingPlanStatusSuccess() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        trainingPlan.setType(TrainingPlanType.PLANNED_WORKOUT);
        trainingPlan.setStatus(TrainingPlanStatus.PLANNED);
        trainingPlan.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlanStatus(
                USER_EMAIL,
                "plan-1",
                TrainingPlanStatus.DONE
        );

        assertThat(updatedTrainingPlan.getStatus()).isEqualTo(TrainingPlanStatus.DONE);
        verify(trainingPlanRepository).save(trainingPlan);
    }

    @Test
    @DisplayName("should reject invalid training plan status transition")
    void updateTrainingPlanStatusRejectsInvalidTransition() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        trainingPlan.setType(TrainingPlanType.PLANNED_WORKOUT);
        trainingPlan.setStatus(TrainingPlanStatus.DONE);
        trainingPlan.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));

        assertThatThrownBy(() -> trainingPlanService.updateTrainingPlanStatus(USER_EMAIL, "plan-1", TrainingPlanStatus.SKIPPED))
                .isInstanceOf(InvalidTrainingPlanStatusTransitionException.class)
                .hasMessage("Training plan status cannot transition from DONE to SKIPPED.");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should return not found when training plan status update targets another user")
    void updateTrainingPlanStatusRejectsForeignPlan() {
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingPlanService.updateTrainingPlanStatus(USER_EMAIL, "plan-1", TrainingPlanStatus.DONE))
                .isInstanceOf(TrainingPlanNotFoundException.class)
                .hasMessage("Training plan not found.");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject training plan status update for template")
    void updateTrainingPlanStatusRejectsTemplate() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));

        assertThatThrownBy(() -> trainingPlanService.updateTrainingPlanStatus(USER_EMAIL, "plan-1", TrainingPlanStatus.DONE))
                .isInstanceOf(TrainingPlanStatusNotAllowedException.class)
                .hasMessage("Training plan status can be updated only for PLANNED_WORKOUT.");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should update training plan status from planned to skipped")
    void updateTrainingPlanStatusToSkippedSuccess() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"), entry("exercise-1", 1, plannedSet(1)));
        trainingPlan.setType(TrainingPlanType.PLANNED_WORKOUT);
        trainingPlan.setStatus(TrainingPlanStatus.PLANNED);
        trainingPlan.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument updatedTrainingPlan = trainingPlanService.updateTrainingPlanStatus(
                USER_EMAIL,
                "plan-1",
                TrainingPlanStatus.SKIPPED
        );

        assertThat(updatedTrainingPlan.getStatus()).isEqualTo(TrainingPlanStatus.SKIPPED);
        verify(trainingPlanRepository).save(trainingPlan);
    }

    @Test
    @DisplayName("should start planned workout as workout and mark plan done")
    void startTrainingPlanCreatesWorkoutAndMarksPlannedWorkoutDone() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                "plan-1",
                "Push A",
                Instant.parse("2026-04-07T12:00:00Z"),
                entry("exercise-1", 1, plannedSet(1), plannedSet(2))
        );
        trainingPlan.setType(TrainingPlanType.PLANNED_WORKOUT);
        trainingPlan.setStatus(TrainingPlanStatus.PLANNED);
        trainingPlan.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));
        when(workoutRepository.save(any(WorkoutDocument.class))).thenAnswer(invocation -> {
            WorkoutDocument workout = invocation.getArgument(0);
            workout.setId("workout-1");
            return workout;
        });
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        var response = trainingPlanService.startTrainingPlan(USER_EMAIL, "plan-1");
        Instant after = Instant.now();

        ArgumentCaptor<WorkoutDocument> workoutCaptor = ArgumentCaptor.forClass(WorkoutDocument.class);
        verify(workoutRepository).save(workoutCaptor.capture());
        WorkoutDocument savedWorkout = workoutCaptor.getValue();
        assertThat(response.workoutId()).isEqualTo("workout-1");
        assertThat(response.trainingPlanId()).isEqualTo("plan-1");
        assertThat(savedWorkout.getUserId()).isEqualTo(USER_ID);
        assertThat(savedWorkout.getName()).isEqualTo("Push A");
        assertThat(savedWorkout.getWorkoutDate()).isBetween(before, after);
        assertThat(savedWorkout.getEntries()).hasSize(1);
        assertThat(savedWorkout.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-1");
        assertThat(savedWorkout.getEntries().getFirst().getSets()).hasSize(2);
        assertThat(savedWorkout.getEntries().getFirst().getSets().getFirst().getType()).isEqualTo(SetType.NORMAL);
        assertThat(trainingPlan.getStatus()).isEqualTo(TrainingPlanStatus.DONE);
        verify(trainingPlanRepository).save(trainingPlan);
    }

    @Test
    @DisplayName("should reject start when plan belongs to another user")
    void startTrainingPlanRejectsForeignPlan() {
        when(trainingPlanRepository.findByIdAndUserId("foreign-plan", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingPlanService.startTrainingPlan(USER_EMAIL, "foreign-plan"))
                .isInstanceOf(TrainingPlanNotFoundException.class)
                .hasMessage("Training plan not found.");

        verify(workoutRepository, never()).save(any());
        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject start when plan has no exercises")
    void startTrainingPlanRejectsEmptyEntries() {
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", Instant.parse("2026-04-07T12:00:00Z"));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));

        assertThatThrownBy(() -> trainingPlanService.startTrainingPlan(USER_EMAIL, "plan-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan has no exercises.");

        verify(workoutRepository, never()).save(any());
        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject start when planned workout is already done")
    void startTrainingPlanRejectsDoneStatus() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                "plan-1",
                "Push A",
                Instant.parse("2026-04-07T12:00:00Z"),
                entry("exercise-1", 1, plannedSet(1))
        );
        trainingPlan.setType(TrainingPlanType.PLANNED_WORKOUT);
        trainingPlan.setStatus(TrainingPlanStatus.DONE);
        trainingPlan.setPlannedDate(LocalDate.parse("2026-04-12"));
        when(trainingPlanRepository.findByIdAndUserId("plan-1", USER_ID)).thenReturn(Optional.of(trainingPlan));

        assertThatThrownBy(() -> trainingPlanService.startTrainingPlan(USER_EMAIL, "plan-1"))
                .isInstanceOf(InvalidTrainingPlanStatusTransitionException.class)
                .hasMessage("Training plan cannot be started from status DONE.");

        verify(workoutRepository, never()).save(any());
        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject training plan create when batch result is missing requested exercise")
    void createTrainingPlanRejectsMissingExercise() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.TEMPLATE,
                null,
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1)),
                entryRequest("missing-exercise", 2, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1", "missing-exercise")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-1", "Bench Press")));

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(USER_EMAIL, request))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessage("Exercise not found.");

        verify(exerciseRepository).findAllByIdInAndUserId(Set.of("exercise-1", "missing-exercise"), USER_ID);
        verify(exerciseRepository, never()).findByIdAndUserId(anyString(), anyString());
        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject training plan create when exercise does not belong to authenticated user")
    void createTrainingPlanRejectsForeignExercise() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.TEMPLATE,
                null,
                true,
                entryRequest("foreign-exercise", 1, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("foreign-exercise")), eq(USER_ID)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(USER_EMAIL, request))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessage("Exercise not found.");

        verify(exerciseRepository).findAllByIdInAndUserId(Set.of("foreign-exercise"), USER_ID);
        verify(exerciseRepository, never()).findByIdAndUserId(anyString(), anyString());
        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should exclude null and blank exercise ids from batch lookup and still reject invalid entry")
    void createTrainingPlanRejectsNullAndBlankExerciseIds() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.TEMPLATE,
                null,
                true,
                entryRequest(null, 1, plannedSetRequest(1)),
                entryRequest("   ", 2, plannedSetRequest(1)),
                entryRequest(" exercise-1 ", 3, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-1", "Bench Press")));

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(USER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan entry exerciseId must not be blank.");

        verify(exerciseRepository).findAllByIdInAndUserId(Set.of("exercise-1"), USER_ID);
        verify(exerciseRepository, never()).findByIdAndUserId(anyString(), anyString());
        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should batch duplicate exercise ids only once and resolve all entries")
    void createTrainingPlanHandlesDuplicateExerciseIds() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.TEMPLATE,
                null,
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1)),
                entryRequest(" exercise-1 ", 2, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-1", "Bench Press")));
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument createdTrainingPlan = trainingPlanService.createTrainingPlan(USER_EMAIL, request);

        assertThat(createdTrainingPlan.getEntries()).hasSize(2);
        assertThat(createdTrainingPlan.getEntries())
                .extracting(TrainingPlanEntry::getExerciseId)
                .containsExactly("exercise-1", "exercise-1");
        assertThat(createdTrainingPlan.getEntries())
                .extracting(TrainingPlanEntry::getExerciseNameSnapshot)
                .containsExactly("Bench Press", "Bench Press");
        verify(exerciseRepository).findAllByIdInAndUserId(Set.of("exercise-1"), USER_ID);
        verify(exerciseRepository, never()).findByIdAndUserId(anyString(), anyString());
    }

    @Test
    @DisplayName("should reject template with planned date")
    void createTrainingPlanRejectsTemplateWithPlannedDate() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.TEMPLATE,
                LocalDate.parse("2026-04-12"),
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-1", "Bench Press")));

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(USER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan of type TEMPLATE must not define plannedDate.");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject planned workout without planned date")
    void createTrainingPlanRejectsPlannedWorkoutWithoutPlannedDate() {
        CreateTrainingPlanRequest request = createRequest(
                "Push A",
                "Upper body",
                TrainingPlanType.PLANNED_WORKOUT,
                null,
                true,
                entryRequest("exercise-1", 1, plannedSetRequest(1))
        );
        when(exerciseRepository.findAllByIdInAndUserId(eq(Set.of("exercise-1")), eq(USER_ID)))
                .thenReturn(List.of(exercise("exercise-1", "Bench Press")));

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(USER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan of type PLANNED_WORKOUT requires plannedDate.");

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
                createRequest("Push A", null, TrainingPlanType.TEMPLATE, null, true, entryRequest("exercise-1", 1, plannedSetRequest(1)))
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
                .type(TrainingPlanType.TEMPLATE)
                .status(null)
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
                                                    TrainingPlanType type,
                                                    LocalDate plannedDate,
                                                    boolean active,
                                                    TrainingPlanEntryRequest... entries) {
        return new CreateTrainingPlanRequest(name, description, type, plannedDate, List.of(entries), active);
    }

    private UpdateTrainingPlanRequest updateRequest(String name,
                                                    String description,
                                                    TrainingPlanType type,
                                                    LocalDate plannedDate,
                                                    boolean active,
                                                    TrainingPlanEntryRequest... entries) {
        return new UpdateTrainingPlanRequest(name, description, type, plannedDate, List.of(entries), active);
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
