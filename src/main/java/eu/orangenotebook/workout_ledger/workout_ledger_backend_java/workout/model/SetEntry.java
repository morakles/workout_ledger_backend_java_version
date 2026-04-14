package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model;

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
public class SetEntry {

    @NotNull
    @Min(1)
    private Integer setNumber;

    @Positive
    private Double weight;

    @Min(1)
    private Integer reps;

    @PositiveOrZero
    private Integer restSeconds;

    @PositiveOrZero
    private Double durationSeconds;

    @PositiveOrZero
    private Double distanceMeters;

    @NotNull
    @Builder.Default
    private SetType type = SetType.NORMAL;
}
