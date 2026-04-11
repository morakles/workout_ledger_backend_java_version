package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutMapper workoutMapper;

    public WorkoutResponse createWorkout(String authenticatedEmail, CreateWorkoutRequest request) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        ensureExercisesBelongToUser(user.getId(), request.entries());

        WorkoutDocument workoutDocument = workoutMapper.toDocument(user.getId(), request);
        WorkoutDocument savedWorkout = workoutRepository.save(workoutDocument);
        return workoutMapper.toResponse(savedWorkout);
    }

    private UserDocument getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found."));
    }

    private void ensureExercisesBelongToUser(String userId, List<CreateWorkoutEntryRequest> entries) {
        Set<String> exerciseIds = entries.stream()
                .map(CreateWorkoutEntryRequest::exerciseId)
                .map(this::trimToNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (exerciseIds.contains(null)) {
            throw new IllegalArgumentException("Workout contains an unknown exercise.");
        }

        List<ExerciseDocument> ownedExercises = exerciseRepository.findAllByIdInAndUserId(exerciseIds, userId);
        if (ownedExercises.size() != exerciseIds.size()) {
            throw new IllegalArgumentException("Workout contains an unknown exercise.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
