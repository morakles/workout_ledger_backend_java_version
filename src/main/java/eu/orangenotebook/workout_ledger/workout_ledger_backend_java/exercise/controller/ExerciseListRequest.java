package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

public record ExerciseListRequest(
        String page,
        String size,
        String sort,
        String direction
) {
}
