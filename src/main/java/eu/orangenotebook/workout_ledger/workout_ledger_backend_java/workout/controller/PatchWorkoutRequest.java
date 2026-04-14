package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record PatchWorkoutRequest(
        @Size(max = 120) String name,
        Instant workoutDate,
        @Size(min = 1) List<@NotNull @Valid CreateWorkoutEntryRequest> entries
) {
    public boolean isEmpty() {
        return name == null && workoutDate == null && entries == null;
    }
}
