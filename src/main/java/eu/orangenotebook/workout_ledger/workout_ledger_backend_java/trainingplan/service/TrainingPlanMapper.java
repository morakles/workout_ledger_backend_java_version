package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.CreateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.PlannedSetRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.PlannedSetResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanEntryResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanListItemResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.TrainingPlanResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller.UpdateTrainingPlanRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.TrimUtil.trimToNull;

@Component
public class TrainingPlanMapper {

    public TrainingPlanDocument toDocument(String userId,
                                           CreateTrainingPlanRequest request,
                                           List<TrainingPlanEntry> entries) {
        return TrainingPlanDocument.builder()
                .userId(trimToNull(userId))
                .name(trimToNull(request.name()))
                .description(trimToNull(request.description()))
                .type(request.type())
                .plannedDate(request.plannedDate())
                .entries(entries)
                .active(request.active())
                .build();
    }

    public void updateDocument(TrainingPlanDocument trainingPlanDocument,
                               UpdateTrainingPlanRequest request,
                               List<TrainingPlanEntry> entries) {
        trainingPlanDocument.setName(trimToNull(request.name()));
        trainingPlanDocument.setDescription(trimToNull(request.description()));
        trainingPlanDocument.setType(request.type());
        trainingPlanDocument.setPlannedDate(request.plannedDate());
        trainingPlanDocument.setEntries(entries);
        trainingPlanDocument.setActive(request.active());
    }

    public TrainingPlanResponse toResponse(TrainingPlanDocument trainingPlanDocument) {
        return new TrainingPlanResponse(
                trainingPlanDocument.getId(),
                trainingPlanDocument.getName(),
                trainingPlanDocument.getDescription(),
                resolveType(trainingPlanDocument),
                resolveStatus(trainingPlanDocument),
                trainingPlanDocument.getPlannedDate(),
                trainingPlanDocument.getEntries().stream()
                        .map(this::toEntryResponse)
                        .toList(),
                trainingPlanDocument.isActive(),
                trainingPlanDocument.getCreatedAt(),
                trainingPlanDocument.getUpdatedAt()
        );
    }

    public TrainingPlanListItemResponse toListItemResponse(TrainingPlanDocument trainingPlanDocument) {
        return new TrainingPlanListItemResponse(
                trainingPlanDocument.getId(),
                trainingPlanDocument.getName(),
                resolveType(trainingPlanDocument),
                resolveStatus(trainingPlanDocument),
                trainingPlanDocument.getPlannedDate()
        );
    }

    public TrainingPlanEntry toEntry(TrainingPlanEntryRequest request, ExerciseDocument exerciseDocument) {
        return TrainingPlanEntry.builder()
                .exerciseId(trimToNull(exerciseDocument.getId()))
                .exerciseNameSnapshot(trimToNull(exerciseDocument.getName()))
                .order(request.order())
                .notes(trimToNull(request.notes()))
                .plannedSets(Optional.ofNullable(request.plannedSets())
                        .orElseGet(List::of)
                        .stream()
                        .map(this::toPlannedSet)
                        .toList())
                .build();
    }

    private PlannedSet toPlannedSet(PlannedSetRequest request) {
        return PlannedSet.builder()
                .setNumber(request.setNumber())
                .reps(request.reps())
                .weight(request.weight())
                .durationSeconds(request.durationSeconds())
                .distanceMeters(request.distanceMeters())
                .restSeconds(request.restSeconds())
                .type(request.type())
                .build();
    }

    private TrainingPlanEntryResponse toEntryResponse(TrainingPlanEntry trainingPlanEntry) {
        return new TrainingPlanEntryResponse(
                trainingPlanEntry.getExerciseId(),
                trainingPlanEntry.getExerciseNameSnapshot(),
                trainingPlanEntry.getOrder(),
                trainingPlanEntry.getNotes(),
                trainingPlanEntry.getPlannedSets().stream()
                        .map(this::toPlannedSetResponse)
                        .toList()
        );
    }

    private PlannedSetResponse toPlannedSetResponse(PlannedSet plannedSet) {
        return new PlannedSetResponse(
                plannedSet.getSetNumber(),
                plannedSet.getReps(),
                plannedSet.getWeight(),
                plannedSet.getDurationSeconds(),
                plannedSet.getDistanceMeters(),
                plannedSet.getRestSeconds(),
                plannedSet.getType()
        );
    }

    private TrainingPlanType resolveType(TrainingPlanDocument trainingPlanDocument) {
        return Optional.ofNullable(trainingPlanDocument.getType())
                .orElse(TrainingPlanType.TEMPLATE);
    }

    private TrainingPlanStatus resolveStatus(TrainingPlanDocument trainingPlanDocument) {
        return trainingPlanDocument.getStatus();
    }
}
