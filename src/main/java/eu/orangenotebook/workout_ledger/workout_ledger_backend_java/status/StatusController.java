package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.status;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/status")
public class StatusController {

    @GetMapping(produces = "application/json")
    public ResponseEntity<String> status(){
        return ResponseEntity.ok("=== OK ===");
    }
}

