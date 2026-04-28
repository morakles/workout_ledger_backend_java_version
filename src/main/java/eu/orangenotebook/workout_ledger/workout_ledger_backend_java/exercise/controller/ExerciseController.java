package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.config.ApiPaths;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/exercises")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping
    public ResponseEntity<ExerciseResponse> createExercise(@Valid @RequestBody CreateExerciseRequest request,
                                                           Authentication authentication) {
        var createdExercise = exerciseService.createExercise(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(createdExercise));
    }

    @GetMapping
    public ResponseEntity<List<ExerciseListItemResponse>> getExercises(Authentication authentication) {
        List<ExerciseListItemResponse> exercises = exerciseService.listExercises(authentication.getName()).stream()
                .map(ExerciseListItemResponse::from)
                .toList();
        return ResponseEntity.ok(exercises);
    }

    @PutMapping("/{exerciseId}")
    public ResponseEntity<ExerciseResponse> updateExercise(@PathVariable String exerciseId,
                                                           @Valid @RequestBody UpdateExerciseRequest request,
                                                           Authentication authentication) {
        var updatedExercise = exerciseService.updateExercise(authentication.getName(), exerciseId, request);
        return ResponseEntity.ok(ExerciseResponse.from(updatedExercise));
    }

    @DeleteMapping("/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(@PathVariable String exerciseId, Authentication authentication) {
        exerciseService.deleteExercise(authentication.getName(), exerciseId);
        return ResponseEntity.noContent().build();
    }
}
