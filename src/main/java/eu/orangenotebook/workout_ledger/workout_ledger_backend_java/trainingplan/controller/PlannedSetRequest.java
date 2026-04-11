package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlannedSetRequest(
        @NotNull @Min(1) Integer setNumber,
        @Positive Integer reps,
        @Positive Double weight,
        @Positive Double durationSeconds,
        @Positive Double distanceMeters,
        @Positive Integer restSeconds,
        @NotNull PlannedSetType type
) {
}
