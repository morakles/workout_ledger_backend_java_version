package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkoutRepository extends MongoRepository<WorkoutDocument, String> {

    boolean existsByEntriesExerciseId(String exerciseId);
}
