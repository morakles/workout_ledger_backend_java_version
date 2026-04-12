package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@Document(collection = "training_plans")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_training_plans_user_active",
                def = "{'userId': 1, 'active': 1}"
        ),
        @CompoundIndex(
                name = "idx_training_plans_user_name",
                def = "{'userId': 1, 'name': 1}"
        )
})
public class TrainingPlanDocument {

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "idx_training_plans_user_id")
    private String userId;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 1000)
    private String description;

    @Valid
    @NotNull
    @Builder.Default
    private List<@Valid TrainingPlanEntry> entries = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
