package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.WorkoutService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    public ResponseEntity<WorkoutResponse> createWorkout(@Valid @RequestBody CreateWorkoutRequest request,
                                                         Authentication authentication) {
        WorkoutResponse createdWorkout = workoutService.createWorkout(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkout);
    }

    @GetMapping
    public ResponseEntity<WorkoutPageResponse> getWorkouts(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(workoutService.getAll(authentication.getName(), page, size));
    }

    @GetMapping("/{workoutId}")
    public ResponseEntity<WorkoutResponse> getWorkout(@PathVariable String workoutId, Authentication authentication) {
        return ResponseEntity.ok(workoutService.getById(workoutId, authentication.getName()));
    }

    @PutMapping("/{workoutId}")
    public ResponseEntity<WorkoutResponse> updateWorkout(@PathVariable String workoutId,
                                                         @Valid @RequestBody CreateWorkoutRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(workoutService.update(workoutId, request, authentication.getName()));
    }

    @PatchMapping("/{workoutId}")
    public ResponseEntity<WorkoutResponse> partiallyUpdateWorkout(@PathVariable String workoutId,
                                                                  @Valid @RequestBody PatchWorkoutRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(workoutService.partialUpdate(workoutId, request, authentication.getName()));
    }

    @DeleteMapping("/{workoutId}")
    public ResponseEntity<Void> deleteWorkout(@PathVariable String workoutId, Authentication authentication) {
        workoutService.delete(workoutId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
