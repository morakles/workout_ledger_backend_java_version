package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workouts")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_workouts_user_workout_date",
                def = "{'userId': 1, 'workoutDate': -1}"
        )
})
public class WorkoutDocument {

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "idx_workouts_user_id")
    private String userId;

    @Size(max = 120)
    private String name;

    @Size(max = 1000)
    private String notes;

    @NotNull
    private Instant workoutDate;

    @Valid
    @NotEmpty
    @Builder.Default
    private List<@Valid WorkoutEntry> entries = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
