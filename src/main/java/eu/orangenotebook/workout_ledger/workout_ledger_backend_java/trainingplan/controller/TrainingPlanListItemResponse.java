package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;

import java.time.LocalDate;

public record TrainingPlanListItemResponse(
        String id,
        String name,
        TrainingPlanType type,
        LocalDate plannedDate
) {
}
