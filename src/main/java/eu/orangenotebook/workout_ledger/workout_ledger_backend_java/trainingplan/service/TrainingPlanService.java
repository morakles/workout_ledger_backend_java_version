package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;

    public TrainingPlanDocument createTrainingPlan(TrainingPlanDocument trainingPlanDocument) {
        validateTrainingPlan(trainingPlanDocument);
        return trainingPlanRepository.save(trainingPlanDocument);
    }

    public TrainingPlanDocument updateTrainingPlan(TrainingPlanDocument trainingPlanDocument) {
        validateTrainingPlan(trainingPlanDocument);
        return trainingPlanRepository.save(trainingPlanDocument);
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
