package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateWorkoutEntryRequest(
        @NotBlank @Size(max = 64) String exerciseId,
        @Size(max = 1000) String notes,
        @NotEmpty List<@Valid CreateSetEntryRequest> sets
) {
}
