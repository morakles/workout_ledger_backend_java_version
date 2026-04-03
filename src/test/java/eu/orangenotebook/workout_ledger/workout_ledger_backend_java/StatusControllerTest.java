package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.status.StatusController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusControllerTest {

    @Test
    @DisplayName("status() returns OK and timestamp")
    void statusMethodReturnsExpectedPayload() {
        StatusController controller = new StatusController();

        ResponseEntity<?> response = controller.status();
        Object body = response.getBody();

        assertNotNull(body, "Response body should not be null");
        assertEquals(200, response.getStatusCode().value());
        StatusController.StatusResponse payload = (StatusController.StatusResponse) body;
        assertEquals("OK", payload.status());
        assertNotNull(payload.timestamp(), "Timestamp should be present");
        assertEquals(true, payload.timestamp().isBefore(Instant.now().plusSeconds(5)));
    }
}

