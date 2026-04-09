package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
