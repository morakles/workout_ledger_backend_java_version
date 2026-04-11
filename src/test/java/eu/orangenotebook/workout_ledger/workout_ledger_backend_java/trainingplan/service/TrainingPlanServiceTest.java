package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.repository.TrainingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    private TrainingPlanService trainingPlanService;

    @BeforeEach
    void setUp() {
        trainingPlanService = new TrainingPlanService(trainingPlanRepository);
    }

    @Test
    @DisplayName("should create training plan when entry orders and set numbers are unique")
    void createTrainingPlanSuccess() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                entry("exercise-1", 1, plannedSet(1), plannedSet(2)),
                entry("exercise-2", 2, plannedSet(1))
        );
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument savedTrainingPlan = trainingPlanService.createTrainingPlan(trainingPlan);

        ArgumentCaptor<TrainingPlanDocument> trainingPlanCaptor = ArgumentCaptor.forClass(TrainingPlanDocument.class);
        verify(trainingPlanRepository).save(trainingPlanCaptor.capture());
        assertThat(trainingPlanCaptor.getValue()).isSameAs(trainingPlan);
        assertThat(savedTrainingPlan).isSameAs(trainingPlan);
    }

    @Test
    @DisplayName("should reject training plan update when entry order is duplicated")
    void updateTrainingPlanRejectsDuplicatedEntryOrder() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                entry("exercise-1", 2, plannedSet(1)),
                entry("exercise-2", 2, plannedSet(1))
        );

        assertThatThrownBy(() -> trainingPlanService.updateTrainingPlan(trainingPlan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan contains duplicated entry order: 2");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject training plan create when set number is duplicated within single entry")
    void createTrainingPlanRejectsDuplicatedSetNumberWithinEntry() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                entry("exercise-1", 1, plannedSet(3), plannedSet(3))
        );

        assertThatThrownBy(() -> trainingPlanService.createTrainingPlan(trainingPlan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training plan entry contains duplicated setNumber: 3 for exerciseId=exercise-1");

        verify(trainingPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("should allow same set number in different entries")
    void updateTrainingPlanAllowsSameSetNumberInDifferentEntries() {
        TrainingPlanDocument trainingPlan = trainingPlan(
                entry("exercise-1", 1, plannedSet(1), plannedSet(2)),
                entry("exercise-2", 2, plannedSet(1), plannedSet(2))
        );
        when(trainingPlanRepository.save(any(TrainingPlanDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlanDocument savedTrainingPlan = trainingPlanService.updateTrainingPlan(trainingPlan);

        verify(trainingPlanRepository).save(trainingPlan);
        assertThat(savedTrainingPlan).isSameAs(trainingPlan);
    }

    private TrainingPlanDocument trainingPlan(TrainingPlanEntry... entries) {
        return TrainingPlanDocument.builder()
                .id("plan-1")
                .userId("user-1")
                .name("Push A")
                .entries(List.of(entries))
                .build();
    }

    private TrainingPlanEntry entry(String exerciseId, Integer order, PlannedSet... plannedSets) {
        return TrainingPlanEntry.builder()
                .exerciseId(exerciseId)
                .exerciseNameSnapshot("Exercise " + exerciseId)
                .order(order)
                .plannedSets(List.of(plannedSets))
                .build();
    }

    private PlannedSet plannedSet(Integer setNumber) {
        return PlannedSet.builder()
                .setNumber(setNumber)
                .reps(10)
                .type(PlannedSetType.NORMAL)
                .build();
    }
}
