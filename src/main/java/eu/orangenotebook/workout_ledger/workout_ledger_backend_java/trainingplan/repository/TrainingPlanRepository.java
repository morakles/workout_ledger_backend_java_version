package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainingPlanRepository extends MongoRepository<TrainingPlanDocument, String> {
}
