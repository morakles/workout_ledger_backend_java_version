package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

@Data
@NoArgsConstructor
@Document(collection = "exercises")
@CompoundIndexes({
        @CompoundIndex(
                name = "ux_exercises_user_name_normalized",
                def = "{'userId': 1, 'nameNormalized': 1}",
                unique = true
        ),
        @CompoundIndex(
                name = "idx_exercises_user_category",
                def = "{'userId': 1, 'category': 1}"
        ),
        @CompoundIndex(
                name = "idx_exercises_user_created_at",
                def = "{'userId': 1, 'createdAt': -1}"
        )
})
public class ExerciseDocument {

    @Id
    private String id;

    @NotBlank
    @Indexed(name = "idx_exercises_user_id")
    private String userId;

    @NotBlank
    private String name;

    @NotBlank
    @Setter(AccessLevel.NONE)
    private String nameNormalized;

    private String description;

    private String category;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Builder
    public ExerciseDocument(
            String id,
            String userId,
            String name,
            String description,
            String category,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        setUserId(userId);
        setName(name);
        this.nameNormalized = normalizeName(this.name);
        setDescription(description);
        setCategory(category);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setName(String name) {
        this.name = trim(name);
        this.nameNormalized = normalizeName(this.name);
    }

    public void setUserId(String userId) {
        this.userId = trim(userId);
    }

    public void setCategory(String category) {
        this.category = trim(category);
    }

    public void setDescription(String description) {
        this.description = trim(description);
    }

    private static String normalizeName(String value) {
        String trimmedValue = trim(value);
        return trimmedValue == null ? null : trimmedValue.toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
