package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GoogleTokenVerifier {

    private final List<String> clientIds;
    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    public GoogleTokenVerifier(@Value("${google.client-id:}") String clientId) {
        this.clientIds = Arrays.stream(clientId.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public String verifyAndExtractEmail(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new IllegalArgumentException("Google idToken is required");
        }
        try {
            GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory);
            if (!clientIds.isEmpty()) {
                builder.setAudience(clientIds);
            }
            GoogleIdTokenVerifier verifier = builder.build();
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new AuthenticationException("Google token verification failed: token invalid or expired");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();
            if (email == null || email.isBlank()) {
                throw new AuthenticationException("Google token verification failed: email is missing");
            }
            if (emailVerified == null || !emailVerified) {
                throw new AuthenticationException("Google account email is not verified");
            }
            return email;
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google token verification failed", e);
            throw new AuthenticationException("Google token verification failed", e);
        }
    }
}
