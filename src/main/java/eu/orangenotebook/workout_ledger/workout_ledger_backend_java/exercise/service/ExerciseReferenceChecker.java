package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service;

public interface ExerciseReferenceChecker {

    boolean isExerciseInUse(String exerciseId);
}
