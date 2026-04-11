package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

public class TrimUtil {
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
