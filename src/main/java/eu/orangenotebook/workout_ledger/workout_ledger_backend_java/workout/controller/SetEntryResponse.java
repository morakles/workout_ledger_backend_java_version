package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;

public record SetEntryResponse(
        Integer setNumber,
        Double weight,
        Integer reps,
        Integer restSeconds,
        Double durationSeconds,
        Double distanceMeters,
        SetType type
) {
}
