package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.WorkoutNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateSetEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.PatchWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutPageResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        validateSetDefinitions(request.entries());
        ensureExercisesBelongToUser(user.getId(), request.entries());

        WorkoutDocument workoutDocument = workoutMapper.toDocument(user.getId(), request);
        WorkoutDocument savedWorkout = workoutRepository.save(workoutDocument);
        return workoutMapper.toResponse(savedWorkout);
    }

    public WorkoutPageResponse getAll(String authenticatedEmail, Integer page, Integer size) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        Pageable pageable = createPageable(page, size);
        Page<WorkoutResponse> workoutPage = workoutRepository.findAllByUserIdOrderByWorkoutDateDesc(user.getId(), pageable)
                .map(workoutMapper::toResponse);
        return WorkoutPageResponse.from(workoutPage);
    }

    public WorkoutResponse getById(String workoutId, String authenticatedEmail) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        return workoutMapper.toResponse(getUserWorkout(workoutId, user.getId()));
    }

    public WorkoutResponse update(String workoutId, CreateWorkoutRequest request, String authenticatedEmail) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        WorkoutDocument workoutDocument = getUserWorkout(workoutId, user.getId());
        validateSetDefinitions(request.entries());
        ensureExercisesBelongToUser(user.getId(), request.entries());
        workoutMapper.updateDocument(workoutDocument, request);
        return workoutMapper.toResponse(workoutRepository.save(workoutDocument));
    }

    public WorkoutResponse partialUpdate(String workoutId, PatchWorkoutRequest request, String authenticatedEmail) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided for partial update.");
        }

        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        WorkoutDocument workoutDocument = getUserWorkout(workoutId, user.getId());
        if (request.entries() != null) {
            validateSetDefinitions(request.entries());
            ensureExercisesBelongToUser(user.getId(), request.entries());
        }
        workoutMapper.partialUpdateDocument(workoutDocument, request);
        return workoutMapper.toResponse(workoutRepository.save(workoutDocument));
    }

    public void delete(String workoutId, String authenticatedEmail) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        long deletedCount = workoutRepository.deleteByIdAndUserId(workoutId, user.getId());
        if (deletedCount == 0) {
            throw new WorkoutNotFoundException("Workout not found.");
        }
    }

    private UserDocument getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found."));
    }

    private WorkoutDocument getUserWorkout(String workoutId, String userId) {
        return workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new WorkoutNotFoundException("Workout not found."));
    }

    private Pageable createPageable(Integer page, Integer size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workoutDate"));
    }

    private void ensureExercisesBelongToUser(String userId, List<CreateWorkoutEntryRequest> entries) {
        Set<String> exerciseIds = entries.stream()
                .map(CreateWorkoutEntryRequest::exerciseId)
                .map(TrimUtil::trimToNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (exerciseIds.contains(null)) {
            throw new IllegalArgumentException("Workout contains an unknown exercise.");
        }

        List<ExerciseDocument> ownedExercises = exerciseRepository.findAllByIdInAndUserId(exerciseIds, userId);
        if (ownedExercises.size() != exerciseIds.size()) {
            throw new IllegalArgumentException("Workout contains an unknown exercise.");
        }
    }

    private void validateSetDefinitions(List<CreateWorkoutEntryRequest> entries) {
        for (CreateWorkoutEntryRequest entry : entries) {
            for (CreateSetEntryRequest set : entry.sets()) {
                validateSetDefinition(set);
            }
        }
    }

    private void validateSetDefinition(CreateSetEntryRequest set) {
        SetType setType = set.type() != null ? set.type() : SetType.NORMAL;

        switch (setType) {
            case TIME -> {
                if (set.durationSeconds() == null || set.durationSeconds() <= 0) {
                    throw new IllegalArgumentException("Set type TIME requires durationSeconds greater than 0.");
                }
                if (set.reps() != null) {
                    throw new IllegalArgumentException("Set type TIME does not support reps.");
                }
                if (set.distanceMeters() != null) {
                    throw new IllegalArgumentException("Set type TIME does not support distanceMeters.");
                }
            }
            case DISTANCE -> {
                if (set.distanceMeters() == null || set.distanceMeters() <= 0) {
                    throw new IllegalArgumentException("Set type DISTANCE requires distanceMeters greater than 0.");
                }
                if (set.reps() != null) {
                    throw new IllegalArgumentException("Set type DISTANCE does not support reps.");
                }
                if (set.durationSeconds() != null) {
                    throw new IllegalArgumentException("Set type DISTANCE does not support durationSeconds.");
                }
            }
            case NORMAL, WARMUP, DROP, FAILURE -> {
                if (set.reps() == null || set.reps() < 1) {
                    throw new IllegalArgumentException("Set type " + setType + " requires reps greater than 0.");
                }
                if (set.durationSeconds() != null) {
                    throw new IllegalArgumentException("Set type " + setType + " does not support durationSeconds.");
                }
                if (set.distanceMeters() != null) {
                    throw new IllegalArgumentException("Set type " + setType + " does not support distanceMeters.");
                }
            }
        }
    }
}
