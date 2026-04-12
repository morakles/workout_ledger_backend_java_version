package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlanEntry {

    @NotBlank
    @Size(max = 64)
    private String exerciseId;

    @NotBlank
    @Size(max = 120)
    private String exerciseNameSnapshot;

    @NotNull
    @Min(1)
    private Integer order;

    @Size(max = 1000)
    private String notes;

    @Valid
    @NotNull
    @Builder.Default
    private List<@Valid PlannedSet> plannedSets = new ArrayList<>();
}
