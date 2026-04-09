package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import jakarta.validation.constraints.NotBlank;

public record CreateExerciseRequest(
        @NotBlank String name,
        @NotBlank String category,
        String description
) {
}
