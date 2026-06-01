package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateSetEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.PatchWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.SetEntryResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutEntryResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutEntry;
import org.springframework.stereotype.Component;

import java.util.List;

import static eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.TrimUtil.trimToNull;

@Component
public class WorkoutMapper {

    public WorkoutDocument toDocument(String userId, CreateWorkoutRequest request) {
        return WorkoutDocument.builder()
                .userId(trimToNull(userId))
                .name(trimToNull(request.name()))
                .notes(trimToNull(request.notes()))
                .workoutDate(request.workoutDate())
                .entries(toWorkoutEntries(request.entries()))
                .build();
    }

    public void updateDocument(WorkoutDocument workoutDocument, CreateWorkoutRequest request) {
        workoutDocument.setName(trimToNull(request.name()));
        workoutDocument.setNotes(trimToNull(request.notes()));
        workoutDocument.setWorkoutDate(request.workoutDate());
        workoutDocument.setEntries(toWorkoutEntries(request.entries()));
    }

    public void partialUpdateDocument(WorkoutDocument workoutDocument, PatchWorkoutRequest request) {
        if (request.hasName()) {
            workoutDocument.setName(trimToNull(request.name()));
        }
        if (request.hasNotes()) {
            workoutDocument.setNotes(trimToNull(request.notes()));
        }
        if (request.workoutDate() != null) {
            workoutDocument.setWorkoutDate(request.workoutDate());
        }
        if (request.entries() != null) {
            workoutDocument.setEntries(toWorkoutEntries(request.entries()));
        }
    }

    public WorkoutResponse toResponse(WorkoutDocument workoutDocument) {
        return new WorkoutResponse(
                workoutDocument.getId(),
                workoutDocument.getName(),
                workoutDocument.getNotes(),
                workoutDocument.getWorkoutDate(),
                workoutDocument.getEntries().stream()
                        .map(this::toWorkoutEntryResponse)
                        .toList(),
                workoutDocument.getCreatedAt(),
                workoutDocument.getUpdatedAt()
        );
    }

    private List<WorkoutEntry> toWorkoutEntries(List<CreateWorkoutEntryRequest> requests) {
        return requests.stream()
                .map(this::toWorkoutEntry)
                .toList();
    }

    private WorkoutEntry toWorkoutEntry(CreateWorkoutEntryRequest request) {
        return WorkoutEntry.builder()
                .exerciseId(trimToNull(request.exerciseId()))
                .notes(trimToNull(request.notes()))
                .sets(request.sets().stream()
                        .map(this::toSetEntry)
                        .toList())
                .build();
    }

    private SetEntry toSetEntry(CreateSetEntryRequest request) {
        return SetEntry.builder()
                .setNumber(request.setNumber())
                .weight(request.weight())
                .reps(request.reps())
                .restSeconds(request.restSeconds())
                .durationSeconds(request.durationSeconds())
                .distanceMeters(request.distanceMeters())
                .type(normalizeSetType(request.type()))
                .build();
    }

    private WorkoutEntryResponse toWorkoutEntryResponse(WorkoutEntry workoutEntry) {
        return new WorkoutEntryResponse(
                workoutEntry.getExerciseId(),
                workoutEntry.getNotes(),
                workoutEntry.getSets().stream()
                        .map(this::toSetEntryResponse)
                        .toList()
        );
    }

    private SetEntryResponse toSetEntryResponse(SetEntry setEntry) {
        return new SetEntryResponse(
                setEntry.getSetNumber(),
                setEntry.getWeight(),
                setEntry.getReps(),
                setEntry.getRestSeconds(),
                setEntry.getDurationSeconds(),
                setEntry.getDistanceMeters(),
                normalizeSetType(setEntry.getType())
        );
    }

    private SetType normalizeSetType(SetType setType) {
        return setType != null ? setType : SetType.NORMAL;
    }
}
