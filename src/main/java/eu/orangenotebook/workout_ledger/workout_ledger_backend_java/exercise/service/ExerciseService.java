package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.AuthenticationException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseAlreadyExistsException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseInUseException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.ExerciseNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller.CreateExerciseRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller.ExerciseListRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "name";
    private static final String DEFAULT_DIRECTION = "asc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt");
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            "name", "nameNormalized",
            "createdAt", "createdAt"
    );

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final List<ExerciseReferenceChecker> exerciseReferenceCheckers;

    public ExerciseDocument createExercise(String authenticatedEmail, CreateExerciseRequest request) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);

        ExerciseDocument exerciseDocument = ExerciseDocument.builder()
                .userId(user.getId())
                .name(request.name())
                .category(request.category())
                .description(request.description())
                .build();

        if (exerciseRepository.existsByUserIdAndNameNormalized(user.getId(), exerciseDocument.getNameNormalized())) {
            throw new ExerciseAlreadyExistsException("Exercise with this name already exists.");
        }

        try {
            return exerciseRepository.save(exerciseDocument);
        } catch (DuplicateKeyException ex) {
            log.info("Exercise create race detected for userId={} nameNormalized={}",
                    user.getId(), exerciseDocument.getNameNormalized());
            throw new ExerciseAlreadyExistsException("Exercise with this name already exists.");
        }
    }

    public Page<ExerciseDocument> getExercises(String authenticatedEmail, ExerciseListRequest request) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        Pageable pageable = createPageable(request);
        return exerciseRepository.findAllByUserId(user.getId(), pageable);
    }

    public void deleteExercise(String authenticatedEmail, String exerciseId) {
        UserDocument user = getAuthenticatedUser(authenticatedEmail);
        ExerciseDocument exercise = exerciseRepository.findByIdAndUserId(exerciseId, user.getId())
                .orElseThrow(() -> new ExerciseNotFoundException("Exercise not found."));

        if (isExerciseInUse(exercise.getId())) {
            throw new ExerciseInUseException("Exercise cannot be deleted because it is used in existing workouts.");
        }

        exerciseRepository.delete(exercise);
    }

    private UserDocument getAuthenticatedUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found."));
    }

    private Pageable createPageable(ExerciseListRequest request) {
        int page = parsePage(request.page());
        int size = parseSize(request.size());
        String requestedSort = normalizeSortField(request.sort());
        Sort.Direction direction = normalizeDirection(request.direction());
        String persistenceSortField = SORT_FIELD_MAPPING.get(requestedSort);

        return PageRequest.of(page, size, Sort.by(direction, persistenceSortField));
    }

    private int parsePage(String rawPage) {
        int page = parseInteger(rawPage, "Page must be a non-negative integer.", DEFAULT_PAGE);
        if (page < 0) {
            throw new IllegalArgumentException("Page must be a non-negative integer.");
        }
        return page;
    }

    private int parseSize(String rawSize) {
        int size = parseInteger(rawSize, "Size must be between 1 and " + MAX_SIZE + ".", DEFAULT_SIZE);
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE + ".");
        }
        return size;
    }

    private int parseInteger(String value, String message, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeSortField(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return DEFAULT_SORT;
        }
        if (!ALLOWED_SORT_FIELDS.contains(rawSort)) {
            throw new IllegalArgumentException("Sort must be one of: name, createdAt.");
        }
        return rawSort;
    }

    private Sort.Direction normalizeDirection(String rawDirection) {
        String value = rawDirection == null || rawDirection.isBlank()
                ? DEFAULT_DIRECTION
                : rawDirection.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new IllegalArgumentException("Direction must be 'asc' or 'desc'.");
        };
    }

    private boolean isExerciseInUse(String exerciseId) {
        return exerciseReferenceCheckers.stream()
                .anyMatch(checker -> checker.isExerciseInUse(exerciseId));
    }
}
