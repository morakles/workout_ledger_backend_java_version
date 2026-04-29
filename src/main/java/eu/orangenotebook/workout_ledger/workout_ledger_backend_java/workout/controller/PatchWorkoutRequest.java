package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.workout.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class PatchWorkoutRequest {

    @Schema(
            description = "Omit to keep the existing name, send null to clear it, or send a string to update it.",
            nullable = true
    )
    @Size(max = 120)
    private String name;

    @Schema(hidden = true)
    private boolean nameProvided;

    private Instant workoutDate;

    @Size(min = 1)
    private List<@NotNull @Valid CreateWorkoutEntryRequest> entries;

    public PatchWorkoutRequest() {
    }

    private PatchWorkoutRequest(
            boolean nameProvided,
            String name,
            Instant workoutDate,
            List<CreateWorkoutEntryRequest> entries
    ) {
        this.nameProvided = nameProvided;
        this.name = name;
        this.workoutDate = workoutDate;
        this.entries = entries;
    }

    public static PatchWorkoutRequest withName(
            String name,
            Instant workoutDate,
            List<CreateWorkoutEntryRequest> entries
    ) {
        return new PatchWorkoutRequest(true, name, workoutDate, entries);
    }

    public static PatchWorkoutRequest withoutName(
            Instant workoutDate,
            List<CreateWorkoutEntryRequest> entries
    ) {
        return new PatchWorkoutRequest(false, null, workoutDate, entries);
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.nameProvided = true;
        this.name = name;
    }

    @Schema(hidden = true)
    public boolean hasName() {
        return nameProvided;
    }

    public Instant workoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(Instant workoutDate) {
        this.workoutDate = workoutDate;
    }

    public List<CreateWorkoutEntryRequest> entries() {
        return entries;
    }

    public void setEntries(List<CreateWorkoutEntryRequest> entries) {
        this.entries = entries;
    }

    @Schema(hidden = true)
    public boolean isEmpty() {
        return !nameProvided && workoutDate == null && entries == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PatchWorkoutRequest that)) {
            return false;
        }
        return nameProvided == that.nameProvided
                && Objects.equals(name, that.name)
                && Objects.equals(workoutDate, that.workoutDate)
                && Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameProvided, name, workoutDate, entries);
    }
}
