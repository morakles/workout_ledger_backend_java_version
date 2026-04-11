package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import java.time.Instant;
import java.util.List;

public record WorkoutResponse(
        String id,
        String name,
        Instant workoutDate,
        List<WorkoutEntryResponse> entries,
        Instant createdAt,
        Instant updatedAt
) {
}
