package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

public record StartTrainingPlanResponse(
        String workoutId,
        String trainingPlanId
) {
}
