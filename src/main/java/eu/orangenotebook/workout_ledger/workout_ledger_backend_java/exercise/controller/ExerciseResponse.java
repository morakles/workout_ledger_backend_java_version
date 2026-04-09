package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;

import java.time.Instant;

public record ExerciseResponse(
        String id,
        String name,
        String description,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExerciseResponse from(ExerciseDocument exerciseDocument) {
        return new ExerciseResponse(
                exerciseDocument.getId(),
                exerciseDocument.getName(),
                exerciseDocument.getDescription(),
                exerciseDocument.getCategory(),
                exerciseDocument.getCreatedAt(),
                exerciseDocument.getUpdatedAt()
        );
    }
}
