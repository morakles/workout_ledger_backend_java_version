package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex,
                                                                    HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ExerciseAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleExerciseAlreadyExists(ExerciseAlreadyExistsException ex,
                                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleExerciseNotFound(ExerciseNotFoundException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(WorkoutNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkoutNotFound(WorkoutNotFoundException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ExerciseInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleExerciseInUse(ExerciseInUseException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(TrainingPlanNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTrainingPlanNotFound(TrainingPlanNotFoundException ex,
                                                                       HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTrainingPlanStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTrainingPlanStatusTransition(
            InvalidTrainingPlanStatusTransitionException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(TrainingPlanStatusNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleTrainingPlanStatusNotAllowed(TrainingPlanStatusNotAllowedException ex,
                                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex,
                                                                          HttpServletRequest request) {
        String message = ex.getAllErrors().stream()
                .findFirst()
                .map(error -> error instanceof DefaultMessageSourceResolvable resolvable
                        ? resolvable.getDefaultMessage()
                        : "Validation failed")
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                         HttpServletRequest request) {
        InvalidFormatException invalidFormatException = findCause(ex, InvalidFormatException.class);
        if (invalidFormatException != null
                && invalidFormatException.getTargetType() != null
                && invalidFormatException.getTargetType().isEnum()) {
            String fieldName = invalidFormatException.getPath().stream()
                    .map(reference -> reference.getFieldName())
                    .filter(field -> field != null && !field.isBlank())
                    .findFirst()
                    .orElse("value");
            String supportedValues = Arrays.stream(invalidFormatException.getTargetType().getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            String message = "Invalid value '" + invalidFormatException.getValue()
                    + "' for field '" + fieldName + "'. Allowed values: [" + supportedValues + "].";
            return buildResponse(HttpStatus.BAD_REQUEST, message, request);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                             HttpServletRequest request) {
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            String supportedValues = Arrays.stream(ex.getRequiredType().getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            String message = "Invalid value '" + ex.getValue()
                    + "' for parameter '" + ex.getName() + "'. Allowed values: [" + supportedValues + "].";
            return buildResponse(HttpStatus.BAD_REQUEST, message, request);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameter.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    public record ApiErrorResponse(Instant timestamp, int status, String error, String message, String path) {
    }
}
