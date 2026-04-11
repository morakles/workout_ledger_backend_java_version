package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import java.util.List;

public record TrainingPlanEntryResponse(
        String exerciseId,
        String exerciseNameSnapshot,
        Integer order,
        String notes,
        List<PlannedSetResponse> plannedSets
) {
}
