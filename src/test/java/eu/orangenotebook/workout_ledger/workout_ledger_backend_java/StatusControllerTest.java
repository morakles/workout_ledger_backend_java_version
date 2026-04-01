package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.status.StatusController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusControllerTest {

    @Test
    @DisplayName("status() returns UP and timestamp")
    void statusMethodReturnsExpectedPayload() {
        StatusController controller = new StatusController();

        Map<String, Object> result = controller.status();

        assertNotNull(result, "Result map should not be null");
        assertEquals("UP", result.get("status"));
        assertNotNull(result.get("timestamp"), "Timestamp should be present");
    }
}

