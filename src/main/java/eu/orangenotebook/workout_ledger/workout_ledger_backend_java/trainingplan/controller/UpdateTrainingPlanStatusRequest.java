package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTrainingPlanStatusRequest(
        @NotNull TrainingPlanStatus status
) {
}
