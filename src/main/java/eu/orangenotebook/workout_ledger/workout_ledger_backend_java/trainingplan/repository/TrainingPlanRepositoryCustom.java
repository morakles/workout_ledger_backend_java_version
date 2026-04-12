package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;

import java.time.LocalDate;
import java.util.List;

public interface TrainingPlanRepositoryCustom {

    List<TrainingPlanDocument> findAllByUserIdAndFilters(String userId,
                                                         TrainingPlanType type,
                                                         LocalDate from,
                                                         LocalDate to);
}
