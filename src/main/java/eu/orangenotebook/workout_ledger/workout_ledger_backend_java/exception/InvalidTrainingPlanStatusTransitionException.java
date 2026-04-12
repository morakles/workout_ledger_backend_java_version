package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class InvalidTrainingPlanStatusTransitionException extends RuntimeException {

    public InvalidTrainingPlanStatusTransitionException(String message) {
        super(message);
    }
}
