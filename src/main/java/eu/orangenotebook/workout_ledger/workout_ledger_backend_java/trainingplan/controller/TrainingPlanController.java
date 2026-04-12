package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service.TrainingPlanMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service.TrainingPlanListQuery;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service.TrainingPlanService;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/training-plans")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;
    private final TrainingPlanMapper trainingPlanMapper;

    @PostMapping
    public ResponseEntity<TrainingPlanResponse> createTrainingPlan(@Valid @RequestBody CreateTrainingPlanRequest request,
                                                                   Authentication authentication) {
        var createdTrainingPlan = trainingPlanService.createTrainingPlan(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(trainingPlanMapper.toResponse(createdTrainingPlan));
    }

    @GetMapping
    public ResponseEntity<List<TrainingPlanResponse>> getTrainingPlans(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) TrainingPlanType type,
            @RequestParam(required = false) TrainingPlanStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(trainingPlanService.listTrainingPlanResponses(
                authentication.getName(),
                new TrainingPlanListQuery(from, to, type, status)
        ));
    }

    @GetMapping("/summaries")
    public ResponseEntity<List<TrainingPlanListItemResponse>> getTrainingPlanSummaries(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) TrainingPlanType type,
            @RequestParam(required = false) TrainingPlanStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(trainingPlanService.listTrainingPlanSummaries(
                authentication.getName(),
                new TrainingPlanListQuery(from, to, type, status)
        ));
    }

    @GetMapping("/{trainingPlanId}")
    public ResponseEntity<TrainingPlanResponse> getTrainingPlan(@PathVariable String trainingPlanId,
                                                                Authentication authentication) {
        var trainingPlan = trainingPlanService.getTrainingPlan(authentication.getName(), trainingPlanId);
        return ResponseEntity.ok(trainingPlanMapper.toResponse(trainingPlan));
    }

    @PutMapping("/{trainingPlanId}")
    public ResponseEntity<TrainingPlanResponse> updateTrainingPlan(@PathVariable String trainingPlanId,
                                                                   @Valid @RequestBody UpdateTrainingPlanRequest request,
                                                                   Authentication authentication) {
        var updatedTrainingPlan = trainingPlanService.updateTrainingPlan(authentication.getName(), trainingPlanId, request);
        return ResponseEntity.ok(trainingPlanMapper.toResponse(updatedTrainingPlan));
    }

    @PatchMapping("/{trainingPlanId}/status")
    public ResponseEntity<TrainingPlanResponse> updateTrainingPlanStatus(@PathVariable String trainingPlanId,
                                                                         @Valid @RequestBody UpdateTrainingPlanStatusRequest request,
                                                                         Authentication authentication) {
        var updatedTrainingPlan = trainingPlanService.updateTrainingPlanStatus(
                authentication.getName(),
                trainingPlanId,
                request.status()
        );
        return ResponseEntity.ok(trainingPlanMapper.toResponse(updatedTrainingPlan));
    }

    @DeleteMapping("/{trainingPlanId}")
    public ResponseEntity<Void> deleteTrainingPlan(@PathVariable String trainingPlanId, Authentication authentication) {
        trainingPlanService.deleteTrainingPlan(authentication.getName(), trainingPlanId);
        return ResponseEntity.noContent().build();
    }
}
