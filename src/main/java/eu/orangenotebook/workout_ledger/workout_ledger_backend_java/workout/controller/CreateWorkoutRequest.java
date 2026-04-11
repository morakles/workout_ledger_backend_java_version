package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateWorkoutRequest(
        @Size(max = 120) String name,
        @NotNull Instant workoutDate,
        @NotEmpty List<@Valid CreateWorkoutEntryRequest> entries
) {
}
