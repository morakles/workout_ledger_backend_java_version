package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service.ExerciseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping
    public ResponseEntity<ExerciseResponse> createExercise(@Valid @RequestBody CreateExerciseRequest request,
                                                           Authentication authentication) {
        ExerciseDocument createdExercise = exerciseService.createExercise(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(createdExercise));
    }

    @GetMapping
    public ResponseEntity<ExercisePageResponse> getExercises(Authentication authentication,
                                                             @RequestParam(defaultValue = "0") String page,
                                                             @RequestParam(defaultValue = "20") String size,
                                                             @RequestParam(required = false) String sort,
                                                             @RequestParam(required = false) String direction) {
        Page<ExerciseDocument> exercisePage = exerciseService.getExercises(
                authentication.getName(),
                new ExerciseListRequest(page, size, sort, direction)
        );
        return ResponseEntity.ok(ExercisePageResponse.from(exercisePage));
    }

    @DeleteMapping("/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(@PathVariable String exerciseId, Authentication authentication) {
        exerciseService.deleteExercise(authentication.getName(), exerciseId);
        return ResponseEntity.noContent().build();
    }
}
