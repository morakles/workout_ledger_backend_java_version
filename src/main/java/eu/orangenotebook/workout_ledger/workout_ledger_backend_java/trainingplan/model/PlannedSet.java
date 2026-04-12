package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @PositiveOrZero
    private Double weight;

    @PositiveOrZero
    private Double durationSeconds;

    @PositiveOrZero
    private Double distanceMeters;

    @PositiveOrZero
    private Integer restSeconds;

    @NotNull
    @Builder.Default
    private PlannedSetType type = PlannedSetType.NORMAL;
}
