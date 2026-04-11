package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class TrainingPlanNotFoundException extends RuntimeException {

    public TrainingPlanNotFoundException(String message) {
        super(message);
    }
}
