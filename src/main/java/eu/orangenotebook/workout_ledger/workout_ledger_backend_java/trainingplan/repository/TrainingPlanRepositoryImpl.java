package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
class TrainingPlanRepositoryImpl implements TrainingPlanRepositoryCustom {

    private static final Date NULL_PLANNED_DATE_SORT_FALLBACK = Date.from(
            LocalDate.of(9999, 12, 31).atStartOfDay(ZoneOffset.UTC).toInstant()
    );

    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;

    @Override
    public List<TrainingPlanDocument> findAllByUserIdAndFilters(String userId,
                                                                TrainingPlanType type,
                                                                TrainingPlanStatus status,
                                                                LocalDate from,
                                                                LocalDate to) {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getObject();
        Criteria matchCriteria = buildMatchCriteria(userId, type, status, from, to);
        Aggregation aggregation = Aggregation.newAggregation(
                context -> new Document("$match", matchCriteria.getCriteriaObject()),
                context -> new Document("$addFields", new Document(
                        "plannedDateSort",
                        new Document("$ifNull", List.of("$plannedDate", NULL_PLANNED_DATE_SORT_FALLBACK))
                )),
                context -> new Document("$sort", new Document("plannedDateSort", 1).append("_id", 1)),
                context -> new Document("$project", new Document("plannedDateSort", 0))
        );

        return mongoTemplate.aggregate(
                        aggregation,
                        mongoTemplate.getCollectionName(TrainingPlanDocument.class),
                        TrainingPlanDocument.class
                )
                .getMappedResults();
    }

    private Criteria buildMatchCriteria(String userId,
                                        TrainingPlanType type,
                                        TrainingPlanStatus status,
                                        LocalDate from,
                                        LocalDate to) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("userId").is(userId));

        if (type == TrainingPlanType.TEMPLATE) {
            criteria.add(new Criteria().orOperator(
                    Criteria.where("type").is(TrainingPlanType.TEMPLATE),
                    Criteria.where("type").is(null)
            ));
        } else if (type != null) {
            criteria.add(Criteria.where("type").is(type));
        }

        if (status != null) {
            criteria.add(Criteria.where("type").is(TrainingPlanType.PLANNED_WORKOUT));
            criteria.add(Criteria.where("status").is(status));
        }

        if (from != null || to != null) {
            Criteria plannedDateCriteria = Criteria.where("plannedDate");
            if (from != null) {
                plannedDateCriteria = plannedDateCriteria.gte(from);
            }
            if (to != null) {
                plannedDateCriteria = plannedDateCriteria.lte(to);
            }
            criteria.add(plannedDateCriteria);
        }

        if (criteria.size() == 1) {
            return criteria.getFirst();
        }

        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }
}
