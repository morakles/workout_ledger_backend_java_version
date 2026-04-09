package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException(String message) {
        super(message);
    }
}
