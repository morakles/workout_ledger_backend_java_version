package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import java.time.Instant;
import java.util.List;

public record TrainingPlanResponse(
        String id,
        String name,
        String description,
        List<TrainingPlanEntryResponse> entries,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
