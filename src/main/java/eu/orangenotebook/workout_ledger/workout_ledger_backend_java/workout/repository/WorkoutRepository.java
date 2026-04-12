package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WorkoutRepository extends MongoRepository<WorkoutDocument, String> {

    Page<WorkoutDocument> findAllByUserIdOrderByWorkoutDateDesc(String userId, Pageable pageable);

    Optional<WorkoutDocument> findByIdAndUserId(String id, String userId);

    long deleteByIdAndUserId(String id, String userId);

    boolean existsByEntriesExerciseId(String exerciseId);
}
