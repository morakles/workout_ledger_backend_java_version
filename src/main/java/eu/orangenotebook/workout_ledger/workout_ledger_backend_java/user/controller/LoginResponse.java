package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller;

public record LoginResponse(
        String token,
        String email
) {
}
