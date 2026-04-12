package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingPlanRepository extends MongoRepository<TrainingPlanDocument, String>,
        TrainingPlanRepositoryCustom {

    List<TrainingPlanDocument> findAllByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<TrainingPlanDocument> findByIdAndUserId(String id, String userId);
}
