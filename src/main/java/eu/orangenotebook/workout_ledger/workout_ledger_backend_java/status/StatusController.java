package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.status;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.config.ApiPaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/status")
public class StatusController {

    @GetMapping(produces = "application/json")
    public ResponseEntity<StatusResponse> status(){
        return ResponseEntity.ok(new StatusResponse("OK", Instant.now()));
    }

    public static record StatusResponse(String status, Instant timestamp) {

    }
}



