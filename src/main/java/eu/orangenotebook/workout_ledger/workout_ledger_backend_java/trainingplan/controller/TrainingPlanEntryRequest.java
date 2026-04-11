package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrainingPlanEntryRequest(
        @NotBlank @Size(max = 64) String exerciseId,
        @NotNull @Min(1) Integer order,
        @Size(max = 1000) String notes,
        @NotNull List<@NotNull @Valid PlannedSetRequest> plannedSets
) {
}
