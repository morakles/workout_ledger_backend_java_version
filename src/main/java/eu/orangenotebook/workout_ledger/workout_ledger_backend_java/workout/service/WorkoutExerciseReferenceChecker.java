package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseReferenceChecker;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkoutExerciseReferenceChecker implements ExerciseReferenceChecker {

    private final WorkoutRepository workoutRepository;

    @Override
    public boolean isExerciseInUse(String exerciseId) {
        return workoutRepository.existsByEntriesExerciseId(exerciseId);
    }
}
