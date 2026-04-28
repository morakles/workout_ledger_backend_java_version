package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.WorkoutNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateSetEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.PatchWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutPageResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    private static final String USER_EMAIL = "user@email.com";
    private static final String USER_ID = "user-1";

    @Mock
    WorkoutRepository workoutRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ExerciseRepository exerciseRepository;

    WorkoutService workoutService;

    @BeforeEach
    void setUp() {
        workoutService = new WorkoutService(
                workoutRepository,
                userRepository,
                exerciseRepository,
                new WorkoutMapper()
        );

        lenient().when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user()));
    }

    @Test
    @DisplayName("should create workout for authenticated user")
    void createWorkoutSuccess() {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                " Push day ",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        " exercise-123 ",
                        " Dobre czucie ",
                        List.of(
                                new CreateSetEntryRequest(1, 60.0, 10, 90, null, null, SetType.NORMAL),
                                new CreateSetEntryRequest(2, 62.5, 8, 90, null, null, SetType.DROP)
                        )
                ))
        );
        when(exerciseRepository.findAllByIdInAndUserId(Set.of("exercise-123"), USER_ID))
                .thenReturn(List.of(exercise("exercise-123")));
        when(workoutRepository.save(any(WorkoutDocument.class))).thenAnswer(invocation -> {
            WorkoutDocument workoutDocument = invocation.getArgument(0);
            workoutDocument.setId("workout-123");
            workoutDocument.setCreatedAt(Instant.parse("2026-04-10T07:00:00Z"));
            workoutDocument.setUpdatedAt(Instant.parse("2026-04-10T07:05:00Z"));
            return workoutDocument;
        });

        WorkoutResponse createdWorkout = workoutService.createWorkout(USER_EMAIL, request);

        ArgumentCaptor<WorkoutDocument> workoutCaptor = ArgumentCaptor.forClass(WorkoutDocument.class);
        verify(workoutRepository).save(workoutCaptor.capture());

        WorkoutDocument savedWorkout = workoutCaptor.getValue();
        assertThat(savedWorkout.getUserId()).isEqualTo(USER_ID);
        assertThat(savedWorkout.getName()).isEqualTo("Push day");
        assertThat(savedWorkout.getWorkoutDate()).isEqualTo(Instant.parse("2026-04-10T06:00:00Z"));
        assertThat(savedWorkout.getEntries()).hasSize(1);
        assertThat(savedWorkout.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-123");
        assertThat(savedWorkout.getEntries().getFirst().getNotes()).isEqualTo("Dobre czucie");
        assertThat(savedWorkout.getEntries().getFirst().getSets()).hasSize(2);
        assertThat(savedWorkout.getEntries().getFirst().getSets().getFirst().getType()).isEqualTo(SetType.NORMAL);

        assertThat(createdWorkout.id()).isEqualTo("workout-123");
        assertThat(createdWorkout.name()).isEqualTo("Push day");
        assertThat(createdWorkout.workoutDate()).isEqualTo(Instant.parse("2026-04-10T06:00:00Z"));
        assertThat(createdWorkout.entries()).hasSize(1);
        assertThat(createdWorkout.entries().getFirst().exerciseId()).isEqualTo("exercise-123");
        assertThat(createdWorkout.entries().getFirst().notes()).isEqualTo("Dobre czucie");
        assertThat(createdWorkout.createdAt()).isEqualTo(Instant.parse("2026-04-10T07:00:00Z"));
        assertThat(createdWorkout.updatedAt()).isEqualTo(Instant.parse("2026-04-10T07:05:00Z"));
    }

    @Test
    @DisplayName("should reject workout when entry references unknown exercise")
    void createWorkoutRejectsUnknownExercise() {
        CreateWorkoutRequest request = createWorkoutRequest(List.of(
                new CreateSetEntryRequest(1, 60.0, 10, 90, null, null, SetType.NORMAL)
        ));
        when(exerciseRepository.findAllByIdInAndUserId(Set.of("exercise-123"), USER_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> workoutService.createWorkout(USER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workout contains an unknown exercise.");

        verify(workoutRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw when authenticated user cannot be resolved")
    void createWorkoutUserMissing() {
        CreateWorkoutRequest request = createWorkoutRequest(List.of(
                new CreateSetEntryRequest(1, 60.0, 10, 90, null, null, SetType.NORMAL)
        ));
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.createWorkout(USER_EMAIL, request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authenticated user not found.");
    }

    @Test
    @DisplayName("should return workout page metadata for authenticated user")
    void getAllReturnsWorkoutPageResponse() {
        WorkoutDocument workoutDocument = workoutDocument();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "workoutDate"));
        when(workoutRepository.findAllByUserIdOrderByWorkoutDateDesc(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(workoutDocument), pageable, 21));

        WorkoutPageResponse workouts = workoutService.getAll(USER_EMAIL, 1, 10);

        assertThat(workouts.items()).hasSize(1);
        assertThat(workouts.items().getFirst().id()).isEqualTo("workout-123");
        assertThat(workouts.page()).isEqualTo(1);
        assertThat(workouts.size()).isEqualTo(10);
        assertThat(workouts.totalElements()).isEqualTo(21);
        assertThat(workouts.totalPages()).isEqualTo(3);
        assertThat(workouts.hasNext()).isTrue();
        assertThat(workouts.hasPrevious()).isTrue();
        verify(workoutRepository).findAllByUserIdOrderByWorkoutDateDesc(USER_ID, pageable);
    }

    @Test
    @DisplayName("should return workout by id only for authenticated user")
    void getByIdSuccess() {
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID))
                .thenReturn(Optional.of(workoutDocument()));

        WorkoutResponse workout = workoutService.getById("workout-123", USER_EMAIL);

        assertThat(workout.id()).isEqualTo("workout-123");
        verify(workoutRepository).findByIdAndUserId("workout-123", USER_ID);
    }

    @Test
    @DisplayName("should throw when workout is not found for authenticated user")
    void getByIdNotFound() {
        when(workoutRepository.findByIdAndUserId("missing-workout", USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.getById("missing-workout", USER_EMAIL))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessage("Workout not found.");
    }

    @Test
    @DisplayName("should fully update workout")
    void updateWorkoutSuccess() {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                " Leg day ",
                Instant.parse("2026-04-11T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                " exercise-456 ",
                " Heavy squats ",
                List.of(new CreateSetEntryRequest(1, 100.0, 5, 180, null, null, SetType.NORMAL))
        ))
        );
        WorkoutDocument existingWorkout = workoutDocument();
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));
        when(exerciseRepository.findAllByIdInAndUserId(Set.of("exercise-456"), USER_ID))
                .thenReturn(List.of(exercise("exercise-456")));
        when(workoutRepository.save(existingWorkout)).thenReturn(existingWorkout);

        WorkoutResponse updatedWorkout = workoutService.update("workout-123", request, USER_EMAIL);

        assertThat(existingWorkout.getName()).isEqualTo("Leg day");
        assertThat(existingWorkout.getWorkoutDate()).isEqualTo(Instant.parse("2026-04-11T06:00:00Z"));
        assertThat(existingWorkout.getEntries()).hasSize(1);
        assertThat(existingWorkout.getEntries().getFirst().getExerciseId()).isEqualTo("exercise-456");
        assertThat(existingWorkout.getEntries().getFirst().getNotes()).isEqualTo("Heavy squats");
        assertThat(updatedWorkout.id()).isEqualTo("workout-123");
    }

    @Test
    @DisplayName("should throw when updating missing workout")
    void updateWorkoutNotFound() {
        CreateWorkoutRequest request = createWorkoutRequest(List.of(
                new CreateSetEntryRequest(1, 60.0, 10, 90, null, null, SetType.NORMAL)
        ));
        when(workoutRepository.findByIdAndUserId("missing-workout", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.update("missing-workout", request, USER_EMAIL))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessage("Workout not found.");
    }

    @Test
    @DisplayName("should partially update provided fields only")
    void partialUpdateSuccess() {
        WorkoutDocument existingWorkout = workoutDocument();
        PatchWorkoutRequest request = PatchWorkoutRequest.withName(" Updated name ", null, null);
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));
        when(workoutRepository.save(existingWorkout)).thenReturn(existingWorkout);

        WorkoutResponse updatedWorkout = workoutService.partialUpdate("workout-123", request, USER_EMAIL);

        assertThat(existingWorkout.getName()).isEqualTo("Updated name");
        assertThat(existingWorkout.getWorkoutDate()).isEqualTo(Instant.parse("2026-04-10T06:00:00Z"));
        assertThat(updatedWorkout.name()).isEqualTo("Updated name");
    }

    @Test
    @DisplayName("should keep existing name when patch omits name")
    void partialUpdateWithoutNameKeepsExistingName() {
        WorkoutDocument existingWorkout = workoutDocument();
        PatchWorkoutRequest request = PatchWorkoutRequest.withoutName(
                Instant.parse("2026-04-12T06:00:00Z"),
                null
        );
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));
        when(workoutRepository.save(existingWorkout)).thenReturn(existingWorkout);

        WorkoutResponse updatedWorkout = workoutService.partialUpdate("workout-123", request, USER_EMAIL);

        assertThat(existingWorkout.getName()).isEqualTo("Push day");
        assertThat(existingWorkout.getWorkoutDate()).isEqualTo(Instant.parse("2026-04-12T06:00:00Z"));
        assertThat(updatedWorkout.name()).isEqualTo("Push day");
    }

    @Test
    @DisplayName("should clear name when patch provides explicit null name")
    void partialUpdateWithNullNameClearsName() {
        WorkoutDocument existingWorkout = workoutDocument();
        PatchWorkoutRequest request = PatchWorkoutRequest.withName(null, null, null);
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));
        when(workoutRepository.save(existingWorkout)).thenReturn(existingWorkout);

        WorkoutResponse updatedWorkout = workoutService.partialUpdate("workout-123", request, USER_EMAIL);

        assertThat(existingWorkout.getName()).isNull();
        assertThat(updatedWorkout.name()).isNull();
    }

    @Test
    @DisplayName("should normalize blank patch name to null")
    void partialUpdateWithBlankNameClearsName() {
        WorkoutDocument existingWorkout = workoutDocument();
        PatchWorkoutRequest request = PatchWorkoutRequest.withName("   ", null, null);
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));
        when(workoutRepository.save(existingWorkout)).thenReturn(existingWorkout);

        WorkoutResponse updatedWorkout = workoutService.partialUpdate("workout-123", request, USER_EMAIL);

        assertThat(existingWorkout.getName()).isNull();
        assertThat(updatedWorkout.name()).isNull();
    }

    @Test
    @DisplayName("should throw when partially updating missing workout")
    void partialUpdateNotFound() {
        PatchWorkoutRequest request = PatchWorkoutRequest.withName(" Updated name ", null, null);
        when(workoutRepository.findByIdAndUserId("missing-workout", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.partialUpdate("missing-workout", request, USER_EMAIL))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessage("Workout not found.");
    }

    @Test
    @DisplayName("should reject partial update without fields")
    void partialUpdateRejectsEmptyRequest() {
        PatchWorkoutRequest request = PatchWorkoutRequest.withoutName(null, null);

        assertThatThrownBy(() -> workoutService.partialUpdate("workout-123", request, USER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one field must be provided for partial update.");

        verifyNoInteractions(workoutRepository);
    }

    @Test
    @DisplayName("should reject normal set without reps")
    void createWorkoutRejectsNormalSetWithoutReps() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, 60.0, null, 90, 60.0, 0.0, SetType.NORMAL),
                "Set type NORMAL requires reps greater than 0."
        );
    }

    @Test
    @DisplayName("should reject warmup set without reps")
    void createWorkoutRejectsWarmupSetWithoutReps() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, 30.0, null, 60, null, null, SetType.WARMUP),
                "Set type WARMUP requires reps greater than 0."
        );
    }

    @Test
    @DisplayName("should reject drop set without reps")
    void createWorkoutRejectsDropSetWithoutReps() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, 20.0, null, 45, null, null, SetType.DROP),
                "Set type DROP requires reps greater than 0."
        );
    }

    @Test
    @DisplayName("should reject time set without duration")
    void createWorkoutRejectsTimeSetWithoutDuration() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, null, null, 45, null, null, SetType.TIME),
                "Set type TIME requires durationSeconds greater than 0."
        );
    }

    @Test
    @DisplayName("should reject time set with reps")
    void createWorkoutRejectsTimeSetWithReps() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, null, 5, 45, 30.0, null, SetType.TIME),
                "Set type TIME does not support reps."
        );
    }

    @Test
    @DisplayName("should reject time set with distance")
    void createWorkoutRejectsTimeSetWithDistance() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, null, null, 45, 30.0, 400.0, SetType.TIME),
                "Set type TIME does not support distanceMeters."
        );
    }

    @Test
    @DisplayName("should reject distance set without distance")
    void createWorkoutRejectsDistanceSetWithoutDistance() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, null, null, 45, null, null, SetType.DISTANCE),
                "Set type DISTANCE requires distanceMeters greater than 0."
        );
    }

    @Test
    @DisplayName("should reject distance set with reps")
    void createWorkoutRejectsDistanceSetWithReps() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, null, 5, 45, null, 400.0, SetType.DISTANCE),
                "Set type DISTANCE does not support reps."
        );
    }

    @Test
    @DisplayName("should reject distance set with duration")
    void createWorkoutRejectsDistanceSetWithDuration() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, null, null, 45, 30.0, 400.0, SetType.DISTANCE),
                "Set type DISTANCE does not support durationSeconds."
        );
    }

    @Test
    @DisplayName("should reject normal set with duration")
    void createWorkoutRejectsNormalSetWithDuration() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, 60.0, 10, 45, 30.0, null, SetType.NORMAL),
                "Set type NORMAL does not support durationSeconds."
        );
    }

    @Test
    @DisplayName("should reject normal set with distance")
    void createWorkoutRejectsNormalSetWithDistance() {
        assertInvalidSet(
                new CreateSetEntryRequest(1, 60.0, 10, 45, null, 400.0, SetType.NORMAL),
                "Set type NORMAL does not support distanceMeters."
        );
    }

    @Test
    @DisplayName("should reject invalid set definition during full update")
    void updateWorkoutRejectsInvalidSetDefinition() {
        CreateWorkoutRequest request = createWorkoutRequest(List.of(
                new CreateSetEntryRequest(1, null, null, 45, 30.0, 400.0, SetType.TIME)
        ));
        WorkoutDocument existingWorkout = workoutDocument();
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));

        assertThatThrownBy(() -> workoutService.update("workout-123", request, USER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Set type TIME does not support distanceMeters.");

        verify(workoutRepository).findByIdAndUserId("workout-123", USER_ID);
        verify(workoutRepository, never()).save(any());
        verifyNoInteractions(exerciseRepository);
    }

    @Test
    @DisplayName("should reject invalid set definition during partial update")
    void partialUpdateRejectsInvalidSetDefinition() {
        PatchWorkoutRequest request = PatchWorkoutRequest.withoutName(
                null,
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        null,
                        List.of(new CreateSetEntryRequest(1, 60.0, 10, 45, 30.0, null, SetType.NORMAL))
                ))
        );
        WorkoutDocument existingWorkout = workoutDocument();
        when(workoutRepository.findByIdAndUserId("workout-123", USER_ID)).thenReturn(Optional.of(existingWorkout));

        assertThatThrownBy(() -> workoutService.partialUpdate("workout-123", request, USER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Set type NORMAL does not support durationSeconds.");

        verify(workoutRepository).findByIdAndUserId("workout-123", USER_ID);
        verify(workoutRepository, never()).save(any());
        verifyNoInteractions(exerciseRepository);
    }

    @Test
    @DisplayName("should delete workout for authenticated user")
    void deleteWorkoutSuccess() {
        when(workoutRepository.deleteByIdAndUserId("workout-123", USER_ID)).thenReturn(1L);

        workoutService.delete("workout-123", USER_EMAIL);

        verify(workoutRepository).deleteByIdAndUserId("workout-123", USER_ID);
    }

    @Test
    @DisplayName("should throw when deleting missing workout")
    void deleteWorkoutNotFound() {
        when(workoutRepository.deleteByIdAndUserId("missing-workout", USER_ID)).thenReturn(0L);

        assertThatThrownBy(() -> workoutService.delete("missing-workout", USER_EMAIL))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessage("Workout not found.");
    }

    private void assertInvalidSet(CreateSetEntryRequest set, String expectedMessage) {
        CreateWorkoutRequest request = createWorkoutRequest(List.of(set));

        assertThatThrownBy(() -> workoutService.createWorkout(USER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

        verifyNoInteractions(exerciseRepository, workoutRepository);
    }

    private CreateWorkoutRequest createWorkoutRequest(List<CreateSetEntryRequest> sets) {
        return new CreateWorkoutRequest(
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        "Dobre czucie",
                        sets
                ))
        );
    }

    private UserDocument user() {
        return UserDocument.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .provider(UserProvider.LOCAL)
                .roles(List.of("ROLE_USER"))
                .build();
    }

    private ExerciseDocument exercise(String exerciseId) {
        return ExerciseDocument.builder()
                .id(exerciseId)
                .userId(USER_ID)
                .name("Bench Press")
                .category("CHEST")
                .build();
    }

    private WorkoutDocument workoutDocument() {
        return WorkoutDocument.builder()
                .id("workout-123")
                .userId(USER_ID)
                .name("Push day")
                .workoutDate(Instant.parse("2026-04-10T06:00:00Z"))
                .entries(List.of(new WorkoutEntry(
                        "exercise-123",
                        "Dobre czucie",
                        List.of(new SetEntry(
                                1,
                                60.0,
                                10,
                                90,
                                60.0,
                                0.0,
                                SetType.NORMAL
                        ))
                )))
                .createdAt(Instant.parse("2026-04-10T07:00:00Z"))
                .updatedAt(Instant.parse("2026-04-10T07:05:00Z"))
                .build();
    }
}
