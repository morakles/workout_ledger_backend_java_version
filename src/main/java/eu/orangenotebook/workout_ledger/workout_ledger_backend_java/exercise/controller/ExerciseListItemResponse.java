package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;

public record ExerciseListItemResponse(
        String id,
        String name,
        String category
) {
    public static ExerciseListItemResponse from(ExerciseDocument exerciseDocument) {
        return new ExerciseListItemResponse(
                exerciseDocument.getId(),
                exerciseDocument.getName(),
                exerciseDocument.getCategory()
        );
    }
}
