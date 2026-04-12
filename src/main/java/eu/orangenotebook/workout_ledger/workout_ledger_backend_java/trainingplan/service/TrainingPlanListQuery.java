package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;

import java.time.LocalDate;

public record TrainingPlanListQuery(
        LocalDate from,
        LocalDate to,
        TrainingPlanType type,
        TrainingPlanStatus status
) {
    public static TrainingPlanListQuery unfiltered() {
        return new TrainingPlanListQuery(null, null, null, null);
    }
}
