package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.service.WorkoutService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workouts")
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
}
