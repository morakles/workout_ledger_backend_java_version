package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class TrainingPlanStatusNotAllowedException extends RuntimeException {

    public TrainingPlanStatusNotAllowedException(String message) {
        super(message);
    }
}
