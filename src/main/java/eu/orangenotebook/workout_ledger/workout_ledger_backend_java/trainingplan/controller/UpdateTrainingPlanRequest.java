package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTrainingPlanRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull List<@NotNull @Valid TrainingPlanEntryRequest> entries,
        @NotNull Boolean active
) {
}
