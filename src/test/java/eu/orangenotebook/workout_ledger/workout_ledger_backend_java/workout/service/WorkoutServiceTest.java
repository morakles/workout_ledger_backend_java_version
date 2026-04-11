package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateSetEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user()));
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
                                new CreateSetEntryRequest(1, 60.0, 10, 90, 60.0, 00.0, SetType.NORMAL  ),
                                new CreateSetEntryRequest(2, 62.5, 8, 90,60.0, 00.0, SetType.DROP)
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
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        "missing-exercise",
                        null,
                        List.of( new CreateSetEntryRequest(1, 60.0, 10, 90, 60.0, 00.0, SetType.NORMAL  ))
                ))
        );
        when(exerciseRepository.findAllByIdInAndUserId(Set.of("missing-exercise"), USER_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> workoutService.createWorkout(USER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workout contains an unknown exercise.");

        verify(workoutRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw when authenticated user cannot be resolved")
    void createWorkoutUserMissing() {
        CreateWorkoutRequest request = new CreateWorkoutRequest(
                "Push day",
                Instant.parse("2026-04-10T06:00:00Z"),
                List.of(new CreateWorkoutEntryRequest(
                        "exercise-123",
                        null,
                        List.of( new CreateSetEntryRequest(1, 60.0, 10, 90, 60.0, 00.0, SetType.NORMAL  ))
                ))
        );
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.createWorkout(USER_EMAIL, request))
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

    private ExerciseDocument exercise(String exerciseId) {
        return ExerciseDocument.builder()
                .id(exerciseId)
                .userId(USER_ID)
                .name("Bench Press")
                .category("CHEST")
                .build();
    }
}
