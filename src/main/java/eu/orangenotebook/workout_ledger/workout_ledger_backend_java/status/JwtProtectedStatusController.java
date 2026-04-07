package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.status;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.Instant;

@RestController
@RequestMapping("/api/protectedstatus")
@SecurityRequirement(name = "Bearer Authentication")
public class JwtProtectedStatusController {

    @GetMapping(produces = "application/json")
    public ResponseEntity<StatusResponse> status(){
        return ResponseEntity.ok(new StatusResponse("Protected status OK", Instant.now()));
    }

    public static record StatusResponse(String status, Instant timestamp) {

    }
}



