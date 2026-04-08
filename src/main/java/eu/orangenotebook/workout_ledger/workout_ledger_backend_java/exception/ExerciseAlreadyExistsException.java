package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class ExerciseAlreadyExistsException extends RuntimeException {

    public ExerciseAlreadyExistsException(String message) {
        super(message);
    }
}
