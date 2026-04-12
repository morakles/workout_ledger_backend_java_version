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
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository.TrainingPlanRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return listTrainingPlans(authenticatedEmail, null, null, null);
    }

    public List<TrainingPlanDocument> listTrainingPlans(String authenticatedEmail,
                                                        LocalDate from,
                                                        LocalDate to,
                                                        TrainingPlanType type) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        validateDateRange(from, to);
        return trainingPlanRepository.findAllByUserIdAndFilters(user.getId(), type, from, to);
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
        List<TrainingPlanEntryRequest> entryRequests = Optional.ofNullable(requests)
                .orElseGet(List::of);
        Set<String> exerciseIds = entryRequests.stream()
                .map(TrainingPlanEntryRequest::exerciseId)
                .map(exerciseId -> trimToNull(exerciseId))
                .filter(exerciseId -> exerciseId != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ExerciseDocument> exercisesById = exerciseIds.isEmpty()
                ? Map.of()
                : exerciseRepository.findAllByIdInAndUserId(exerciseIds, userId).stream()
                        .collect(Collectors.toMap(
                                exercise -> trimToNull(exercise.getId()),
                                Function.identity()
                        ));

        return entryRequests.stream()
                .map(request -> trainingPlanMapper.toEntry(request, getUserExercise(request.exerciseId(), exercisesById)))
                .toList();
    }

    private ExerciseDocument getUserExercise(String exerciseId, Map<String, ExerciseDocument> exercisesById) {
        String trimmedExerciseId = trimToNull(exerciseId);
        if (trimmedExerciseId == null) {
            throw new IllegalArgumentException("Training plan entry exerciseId must not be blank.");
        }

        return Optional.ofNullable(exercisesById.get(trimmedExerciseId))
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
        validateAndNormalizeTypeFields(trainingPlanDocument);
        List<TrainingPlanEntry> entries = trainingPlanDocument.getEntries();
        validateUniqueEntryOrder(entries);
        entries.forEach(this::validateUniqueSetNumbers);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Query param 'from' must be before or equal to 'to'.");
        }
    }

    private void validateAndNormalizeTypeFields(TrainingPlanDocument trainingPlanDocument) {
        TrainingPlanType type = Optional.ofNullable(trainingPlanDocument.getType())
                .orElse(TrainingPlanType.TEMPLATE);
        LocalDate plannedDate = trainingPlanDocument.getPlannedDate();

        if (type == TrainingPlanType.TEMPLATE && plannedDate != null) {
            throw new IllegalArgumentException("Training plan of type TEMPLATE must not define plannedDate.");
        }
        if (type == TrainingPlanType.PLANNED_WORKOUT && plannedDate == null) {
            throw new IllegalArgumentException("Training plan of type PLANNED_WORKOUT requires plannedDate.");
        }

        trainingPlanDocument.setType(type);
        if (type == TrainingPlanType.TEMPLATE) {
            trainingPlanDocument.setPlannedDate(null);
        }
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
