package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateTrainingPlanRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull TrainingPlanType type,
        LocalDate plannedDate,
        @NotNull List<@NotNull @Valid TrainingPlanEntryRequest> entries,
        Boolean active
) {
}
