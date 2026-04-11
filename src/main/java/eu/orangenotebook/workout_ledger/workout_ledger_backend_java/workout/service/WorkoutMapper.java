package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateSetEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutEntryRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.CreateWorkoutRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.SetEntryResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutEntryResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller.WorkoutResponse;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.SetEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model.WorkoutEntry;
import org.springframework.stereotype.Component;

@Component
public class WorkoutMapper {

    public WorkoutDocument toDocument(String userId, CreateWorkoutRequest request) {
        return WorkoutDocument.builder()
                .userId(trimToNull(userId))
                .name(trimToNull(request.name()))
                .workoutDate(request.workoutDate())
                .entries(request.entries().stream()
                        .map(this::toWorkoutEntry)
                        .toList())
                .build();
    }

    public WorkoutResponse toResponse(WorkoutDocument workoutDocument) {
        return new WorkoutResponse(
                workoutDocument.getId(),
                workoutDocument.getName(),
                workoutDocument.getWorkoutDate(),
                workoutDocument.getEntries().stream()
                        .map(this::toWorkoutEntryResponse)
                        .toList(),
                workoutDocument.getCreatedAt(),
                workoutDocument.getUpdatedAt()
        );
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
                .type(request.type())
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
                setEntry.getType()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
