package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import org.springframework.data.domain.Page;

import java.util.List;

public record ExercisePageResponse(
        List<ExerciseResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ExercisePageResponse from(Page<ExerciseDocument> exercisePage) {
        return new ExercisePageResponse(
                exercisePage.getContent().stream()
                        .map(ExerciseResponse::from)
                        .toList(),
                exercisePage.getNumber(),
                exercisePage.getSize(),
                exercisePage.getTotalElements(),
                exercisePage.getTotalPages()
        );
    }
}
