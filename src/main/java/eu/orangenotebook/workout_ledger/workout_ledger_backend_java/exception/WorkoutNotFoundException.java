package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(String message) {
        super(message);
    }
}
