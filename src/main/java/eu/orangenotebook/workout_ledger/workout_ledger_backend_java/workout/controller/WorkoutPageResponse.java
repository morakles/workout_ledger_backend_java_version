package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import org.springframework.data.domain.Page;

import java.util.List;

public record WorkoutPageResponse(
        List<WorkoutResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static WorkoutPageResponse from(Page<WorkoutResponse> workoutPage) {
        return new WorkoutPageResponse(
                workoutPage.getContent(),
                workoutPage.getNumber(),
                workoutPage.getSize(),
                workoutPage.getTotalElements(),
                workoutPage.getTotalPages(),
                workoutPage.hasNext(),
                workoutPage.hasPrevious()
        );
    }
}
