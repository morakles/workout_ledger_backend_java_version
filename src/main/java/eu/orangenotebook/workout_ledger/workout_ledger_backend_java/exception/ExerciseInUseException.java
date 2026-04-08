package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class ExerciseInUseException extends RuntimeException {

    public ExerciseInUseException(String message) {
        super(message);
    }
}
