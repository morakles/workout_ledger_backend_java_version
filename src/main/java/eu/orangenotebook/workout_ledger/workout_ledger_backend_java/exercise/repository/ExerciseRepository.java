package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends MongoRepository<ExerciseDocument, String> {

    boolean existsByUserIdAndNameNormalized(String userId, String nameNormalized);

    List<ExerciseDocument> findAllByUserIdOrderByNameNormalizedAsc(String userId);

    Page<ExerciseDocument> findAllByUserId(String userId, Pageable pageable);

    Optional<ExerciseDocument> findByIdAndUserId(String id, String userId);

    List<ExerciseDocument> findAllByIdInAndUserId(Collection<String> ids, String userId);
}
