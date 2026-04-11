package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import java.util.List;

public record WorkoutEntryResponse(
        String exerciseId,
        String notes,
        List<SetEntryResponse> sets
) {
}
