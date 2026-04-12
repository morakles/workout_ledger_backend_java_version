package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSetType;

public record PlannedSetResponse(
        Integer setNumber,
        Integer reps,
        Double weight,
        Double durationSeconds,
        Double distanceMeters,
        Integer restSeconds,
        PlannedSetType type
) {
}
