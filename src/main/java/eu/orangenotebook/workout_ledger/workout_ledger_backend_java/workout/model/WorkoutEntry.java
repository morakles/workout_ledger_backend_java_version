package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class WorkoutEntry {

    @NotBlank
    @Size(max = 64)
    private String exerciseId;

    @Size(max = 1000)
    private String notes;

    @Valid
    @NotEmpty
    @Builder.Default
    private List<@Valid SetEntry> sets = new ArrayList<>();
}
