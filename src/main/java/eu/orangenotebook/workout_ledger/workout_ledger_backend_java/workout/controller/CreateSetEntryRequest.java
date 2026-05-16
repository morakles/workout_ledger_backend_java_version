package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateSetEntryRequest(
        @NotNull @Min(1) Integer setNumber,
        @PositiveOrZero Double weight,
        @Min(1) Integer reps,
        @PositiveOrZero Integer restSeconds,
        @PositiveOrZero Double durationSeconds,
        @PositiveOrZero Double distanceMeters,
        SetType type
) {
}
