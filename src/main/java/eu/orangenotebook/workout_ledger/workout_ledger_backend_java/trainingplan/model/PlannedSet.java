package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedSet {

    @NotNull
    @Min(1)
    private Integer setNumber;

    @Positive
    private Integer reps;

    @Positive
    private Double weight;

    @Positive
    private Double durationSeconds;

    @Positive
    private Double distanceMeters;

    @Positive
    private Integer restSeconds;

    @NotNull
    @Builder.Default
    private PlannedSetType type = PlannedSetType.NORMAL;
}
