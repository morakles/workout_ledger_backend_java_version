package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @JsonProperty("idToken")
        @JsonAlias("id_token")
        @NotBlank String idToken
) {
}
