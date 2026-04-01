package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class WorkoutLedgerBackendJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkoutLedgerBackendJavaApplication.class, args);
    }

}
