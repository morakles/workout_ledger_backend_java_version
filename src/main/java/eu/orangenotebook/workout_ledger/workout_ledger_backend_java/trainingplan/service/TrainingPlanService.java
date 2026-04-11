package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.TrainingPlanNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.CreateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.UpdateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository.TrainingPlanRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.TrimUtil.trimToNull;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final UserRepository userRepository;
    private final TrainingPlanMapper trainingPlanMapper;
    private final ExerciseRepository exerciseRepository;

    public TrainingPlanDocument createTrainingPlan(String authenticatedEmail, CreateTrainingPlanRequest request) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        List<TrainingPlanEntry> entries = resolveEntries(user.getId(), request.entries());
        TrainingPlanDocument trainingPlanDocument = trainingPlanMapper.toDocument(user.getId(), request, entries);
        return createTrainingPlan(trainingPlanDocument);
    }

    public List<TrainingPlanDocument> listTrainingPlans(String authenticatedEmail) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        return trainingPlanRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    public TrainingPlanDocument getTrainingPlan(String authenticatedEmail, String trainingPlanId) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        return getUserTrainingPlan(trainingPlanId, user.getId());
    }

    public TrainingPlanDocument updateTrainingPlan(String authenticatedEmail,
                                                   String trainingPlanId,
                                                   UpdateTrainingPlanRequest request) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        TrainingPlanDocument trainingPlanDocument = getUserTrainingPlan(trainingPlanId, user.getId());
        List<TrainingPlanEntry> entries = resolveEntries(user.getId(), request.entries());
        trainingPlanMapper.updateDocument(trainingPlanDocument, request, entries);
        return updateTrainingPlan(trainingPlanDocument);
    }

    private List<TrainingPlanEntry> resolveEntries(String userId, List<TrainingPlanEntryRequest> requests) {
        return Optional.ofNullable(requests)
                .orElseGet(List::of)
                .stream()
                .map(request -> trainingPlanMapper.toEntry(request, getUserExercise(request.exerciseId(), userId)))
                .toList();
    }

    private ExerciseDocument getUserExercise(String exerciseId, String userId) {
        String trimmedExerciseId = trimToNull(exerciseId);
        if (trimmedExerciseId == null) {
            throw new IllegalArgumentException("Training plan entry exerciseId must not be blank.");
        }

        return exerciseRepository.findByIdAndUserId(trimmedExerciseId, userId)
                .orElseThrow(() -> new ExerciseNotFoundException("Exercise not found."));
    }

    public void deleteTrainingPlan(String authenticatedEmail, String trainingPlanId) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        TrainingPlanDocument trainingPlanDocument = getUserTrainingPlan(trainingPlanId, user.getId());
        trainingPlanRepository.delete(trainingPlanDocument);
    }

    public TrainingPlanDocument createTrainingPlan(TrainingPlanDocument trainingPlanDocument) {
        validateTrainingPlan(trainingPlanDocument);
        return trainingPlanRepository.save(trainingPlanDocument);
    }

    public TrainingPlanDocument updateTrainingPlan(TrainingPlanDocument trainingPlanDocument) {
        validateTrainingPlan(trainingPlanDocument);
        return trainingPlanRepository.save(trainingPlanDocument);
    }

    private TrainingPlanDocument getUserTrainingPlan(String trainingPlanId, String userId) {
        return trainingPlanRepository.findByIdAndUserId(trainingPlanId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found."));
    }

    private UserDocument getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found."));
    }

    private void validateTrainingPlan(TrainingPlanDocument trainingPlanDocument) {
        List<TrainingPlanEntry> entries = trainingPlanDocument.getEntries();
        validateUniqueEntryOrder(entries);
        entries.forEach(this::validateUniqueSetNumbers);
    }

    private void validateUniqueEntryOrder(List<TrainingPlanEntry> entries) {
        Set<Integer> seenOrders = new HashSet<>();

        for (TrainingPlanEntry entry : entries) {
            Integer order = entry.getOrder();
            if (order != null && !seenOrders.add(order)) {
                throw new IllegalArgumentException("Training plan contains duplicated entry order: " + order);
            }
        }
    }

    private void validateUniqueSetNumbers(TrainingPlanEntry entry) {
        Set<Integer> seenSetNumbers = new HashSet<>();

        for (PlannedSet plannedSet : entry.getPlannedSets()) {
            Integer setNumber = plannedSet.getSetNumber();
            if (setNumber != null && !seenSetNumbers.add(setNumber)) {
                throw new IllegalArgumentException(
                        "Training plan entry contains duplicated setNumber: " + setNumber
                                + " for exerciseId=" + entry.getExerciseId()
                );
            }
        }
    }
}
