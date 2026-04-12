package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TrainingPlanResponse(
        String id,
        String name,
        String description,
        TrainingPlanType type,
        TrainingPlanStatus status,
        LocalDate plannedDate,
        List<TrainingPlanEntryResponse> entries,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
