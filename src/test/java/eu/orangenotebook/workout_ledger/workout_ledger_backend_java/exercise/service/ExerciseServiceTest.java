package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseInUseException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller.ExerciseListRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final String USER_EMAIL = "user@email.com";
    private static final String USER_ID = "user-1";

    @Mock
    ExerciseRepository exerciseRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ExerciseReferenceChecker exerciseReferenceChecker;

    ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        exerciseService = new ExerciseService(
                exerciseRepository,
                userRepository,
                List.of(exerciseReferenceChecker)
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user()));
    }

    @Test
    @DisplayName("should fetch exercises only for authenticated user with default pagination")
    void getExercisesUsesAuthenticatedUserId() {
        when(exerciseRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        exerciseService.getExercises(USER_EMAIL, new ExerciseListRequest("0", "20", null, null));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(exerciseRepository).findAllByUserId(eq(USER_ID), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("nameNormalized")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("nameNormalized").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("should apply custom pagination and sorting")
    void getExercisesAppliesCustomPaginationAndSorting() {
        ExerciseDocument newest = exercise("exercise-2", "Bench Press", Instant.parse("2026-04-07T12:00:00Z"));
        ExerciseDocument older = exercise("exercise-1", "Squat", Instant.parse("2026-04-06T12:00:00Z"));
        when(exerciseRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newest, older)));

        Page<ExerciseDocument> result = exerciseService.getExercises(
                USER_EMAIL,
                new ExerciseListRequest("1", "2", "createdAt", "desc")
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(exerciseRepository).findAllByUserId(eq(USER_ID), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(2);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(result.getContent()).containsExactly(newest, older);
    }

    @Test
    @DisplayName("should reject invalid pagination parameters")
    void getExercisesRejectsInvalidPagination() {
        assertThatThrownBy(() -> exerciseService.getExercises(
                USER_EMAIL,
                new ExerciseListRequest("-1", "101", "name", "asc")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be a non-negative integer.");
    }

    @Test
    @DisplayName("should reject invalid sort field")
    void getExercisesRejectsInvalidSortField() {
        assertThatThrownBy(() -> exerciseService.getExercises(
                USER_EMAIL,
                new ExerciseListRequest("0", "20", "category", "asc")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sort must be one of: name, createdAt.");
    }

    @Test
    @DisplayName("should delete authenticated users exercise")
    void deleteExerciseSuccess() {
        ExerciseDocument exercise = exercise("exercise-1", "Bench Press", Instant.parse("2026-04-07T12:00:00Z"));
        when(exerciseRepository.findByIdAndUserId("exercise-1", USER_ID)).thenReturn(Optional.of(exercise));

        exerciseService.deleteExercise(USER_EMAIL, "exercise-1");

        verify(exerciseReferenceChecker).isExerciseInUse("exercise-1");
        verify(exerciseRepository).delete(exercise);
    }

    @Test
    @DisplayName("should throw when exercise does not exist")
    void deleteExerciseNotFound() {
        when(exerciseRepository.findByIdAndUserId("missing", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.deleteExercise(USER_EMAIL, "missing"))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessage("Exercise not found.");

        verify(exerciseRepository, never()).delete(any());
        verify(exerciseReferenceChecker, never()).isExerciseInUse(anyString());
    }

    @Test
    @DisplayName("should throw when exercise belongs to another user")
    void deleteExerciseForDifferentUser() {
        when(exerciseRepository.findByIdAndUserId("foreign-exercise", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.deleteExercise(USER_EMAIL, "foreign-exercise"))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessage("Exercise not found.");

        verify(exerciseRepository, never()).delete(any());
        verify(exerciseReferenceChecker, never()).isExerciseInUse(anyString());
    }

    @Test
    @DisplayName("should throw when exercise is used in workout data")
    void deleteExerciseInUse() {
        ExerciseDocument exercise = exercise("exercise-1", "Bench Press", Instant.parse("2026-04-07T12:00:00Z"));
        when(exerciseRepository.findByIdAndUserId("exercise-1", USER_ID)).thenReturn(Optional.of(exercise));
        when(exerciseReferenceChecker.isExerciseInUse("exercise-1")).thenReturn(true);

        assertThatThrownBy(() -> exerciseService.deleteExercise(USER_EMAIL, "exercise-1"))
                .isInstanceOf(ExerciseInUseException.class)
                .hasMessage("Exercise cannot be deleted because it is used in existing workouts.");

        verify(exerciseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("should throw when authenticated user cannot be resolved")
    void getExercisesUserMissing() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.getExercises(USER_EMAIL, new ExerciseListRequest("0", "20", null, null)))
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

    private ExerciseDocument exercise(String id, String name, Instant createdAt) {
        return ExerciseDocument.builder()
                .id(id)
                .userId(USER_ID)
                .name(name)
                .category("CHEST")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
