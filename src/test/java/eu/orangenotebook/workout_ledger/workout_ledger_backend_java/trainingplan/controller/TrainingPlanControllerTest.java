package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.GlobalExceptionHandler;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exception.TrainingPlanNotFoundException;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSet;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.PlannedSetType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanEntry;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanStatus;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.model.TrainingPlanType;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service.TrainingPlanListQuery;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service.TrainingPlanMapper;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.trainingplan.service.TrainingPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingPlanControllerTest {

    private static final String USER_EMAIL = "user@email.com";

    @Mock
    TrainingPlanService trainingPlanService;

    TrainingPlanMapper trainingPlanMapper;
    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        trainingPlanMapper = new TrainingPlanMapper();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingPlanController(trainingPlanService, trainingPlanMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("create endpoint should return 201 with created training plan")
    void createTrainingPlanSuccess() throws Exception {
        CreateTrainingPlanRequest request = createTemplateRequest("Push A", true);
        TrainingPlanDocument trainingPlan = trainingPlan("plan-1", "Push A", true);
        when(trainingPlanService.createTrainingPlan(USER_EMAIL, request)).thenReturn(trainingPlan);

        mockMvc.perform(post("/v1/training-plans")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push A"))
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.entries[0].exerciseId").value("exercise-1"));

        verify(trainingPlanService).createTrainingPlan(USER_EMAIL, request);
    }

    @Test
    @DisplayName("list endpoint should return authenticated users training plans")
    void getTrainingPlansSuccess() throws Exception {
        TrainingPlanDocument template = trainingPlan("plan-1", "Push A", TrainingPlanType.TEMPLATE, null, true);
        TrainingPlanDocument plannedWorkout = trainingPlan("plan-2", "Pull A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"), false);
        when(trainingPlanService.listTrainingPlanResponses(USER_EMAIL, TrainingPlanListQuery.unfiltered())).thenReturn(List.of(
                trainingPlanMapper.toResponse(template),
                trainingPlanMapper.toResponse(plannedWorkout)
        ));

        mockMvc.perform(get("/v1/training-plans")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("plan-1"))
                .andExpect(jsonPath("$[0].type").value("TEMPLATE"))
                .andExpect(jsonPath("$[0].status").doesNotExist())
                .andExpect(jsonPath("$[1].id").value("plan-2"))
                .andExpect(jsonPath("$[1].type").value("PLANNED_WORKOUT"))
                .andExpect(jsonPath("$[1].status").value("PLANNED"))
                .andExpect(jsonPath("$[1].plannedDate").value("2026-04-12"));

        verify(trainingPlanService).listTrainingPlanResponses(USER_EMAIL, TrainingPlanListQuery.unfiltered());
    }

    @Test
    @DisplayName("list endpoint should pass date and type filters")
    void getTrainingPlansWithFiltersSuccess() throws Exception {
        LocalDate from = LocalDate.parse("2026-04-10");
        LocalDate to = LocalDate.parse("2026-04-20");
        TrainingPlanListQuery query = new TrainingPlanListQuery(from, to, TrainingPlanType.PLANNED_WORKOUT, null);
        when(trainingPlanService.listTrainingPlanResponses(USER_EMAIL, query))
                .thenReturn(List.of(trainingPlanMapper.toResponse(trainingPlan(
                        "plan-2",
                        "Pull A",
                        TrainingPlanType.PLANNED_WORKOUT,
                        LocalDate.parse("2026-04-12"),
                        false
                ))));

        mockMvc.perform(get("/v1/training-plans")
                        .principal(authentication())
                        .param("from", "2026-04-10")
                        .param("to", "2026-04-20")
                        .param("type", "PLANNED_WORKOUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("plan-2"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"))
                .andExpect(jsonPath("$[0].plannedDate").value("2026-04-12"));

        verify(trainingPlanService).listTrainingPlanResponses(USER_EMAIL, query);
    }

    @Test
    @DisplayName("summary endpoint should return lightweight training plan items")
    void getTrainingPlanSummariesSuccess() throws Exception {
        TrainingPlanDocument template = trainingPlan("plan-1", "Push A", TrainingPlanType.TEMPLATE, null, true);
        TrainingPlanDocument plannedWorkout = trainingPlan("plan-2", "Pull A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"), false);
        when(trainingPlanService.listTrainingPlanSummaries(USER_EMAIL, TrainingPlanListQuery.unfiltered())).thenReturn(List.of(
                trainingPlanMapper.toListItemResponse(template),
                trainingPlanMapper.toListItemResponse(plannedWorkout)
        ));

        mockMvc.perform(get("/v1/training-plans/summaries")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("plan-1"))
                .andExpect(jsonPath("$[0].type").value("TEMPLATE"))
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].entries").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$[1].id").value("plan-2"))
                .andExpect(jsonPath("$[1].type").value("PLANNED_WORKOUT"))
                .andExpect(jsonPath("$[1].status").value("PLANNED"))
                .andExpect(jsonPath("$[1].plannedDate").value("2026-04-12"));

        verify(trainingPlanService).listTrainingPlanSummaries(USER_EMAIL, TrainingPlanListQuery.unfiltered());
    }

    @Test
    @DisplayName("get by id endpoint should return 200 with training plan")
    void getTrainingPlanSuccess() throws Exception {
        when(trainingPlanService.getTrainingPlan(USER_EMAIL, "plan-1")).thenReturn(trainingPlan("plan-1", "Push A", true));

        mockMvc.perform(get("/v1/training-plans/plan-1")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push A"))
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    @DisplayName("get by id endpoint should return 404 when training plan is missing")
    void getTrainingPlanMissing() throws Exception {
        when(trainingPlanService.getTrainingPlan(USER_EMAIL, "missing"))
                .thenThrow(new TrainingPlanNotFoundException("Training plan not found."));

        mockMvc.perform(get("/v1/training-plans/missing")
                        .principal(authentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training plan not found."));
    }

    @Test
    @DisplayName("update endpoint should return 200 with updated training plan")
    void updateTrainingPlanSuccess() throws Exception {
        UpdateTrainingPlanRequest request = updateTemplateRequest("Push B", false);
        when(trainingPlanService.updateTrainingPlan(USER_EMAIL, "plan-1", request))
                .thenReturn(trainingPlan("plan-1", "Push B", false));

        mockMvc.perform(put("/v1/training-plans/plan-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.name").value("Push B"))
                .andExpect(jsonPath("$.type").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.active").value(false));

        verify(trainingPlanService).updateTrainingPlan(USER_EMAIL, "plan-1", request);
    }

    @Test
    @DisplayName("status update endpoint should return 200 with updated training plan")
    void updateTrainingPlanStatusSuccess() throws Exception {
        UpdateTrainingPlanStatusRequest request = new UpdateTrainingPlanStatusRequest(TrainingPlanStatus.DONE);
        when(trainingPlanService.updateTrainingPlanStatus(USER_EMAIL, "plan-1", TrainingPlanStatus.DONE))
                .thenReturn(trainingPlan("plan-1", "Push A", TrainingPlanType.PLANNED_WORKOUT, LocalDate.parse("2026-04-12"),
                        TrainingPlanStatus.DONE, true));

        mockMvc.perform(patch("/v1/training-plans/plan-1/status")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.status").value("DONE"));

        verify(trainingPlanService).updateTrainingPlanStatus(USER_EMAIL, "plan-1", TrainingPlanStatus.DONE);
    }

    @Test
    @DisplayName("delete endpoint should return 204")
    void deleteTrainingPlanSuccess() throws Exception {
        mockMvc.perform(delete("/v1/training-plans/plan-1")
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(trainingPlanService).deleteTrainingPlan(USER_EMAIL, "plan-1");
    }

    @Test
    @DisplayName("create endpoint should reject invalid payload")
    void createTrainingPlanValidationFailure() throws Exception {
        CreateTrainingPlanRequest request = new CreateTrainingPlanRequest(" ", null, null, null, List.of(), null);

        mockMvc.perform(post("/v1/training-plans")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(trainingPlanService);
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(USER_EMAIL, "n/a", List.of());
    }

    private CreateTrainingPlanRequest createTemplateRequest(String name, boolean active) {
        return new CreateTrainingPlanRequest(
                name,
                "Upper body",
                TrainingPlanType.TEMPLATE,
                null,
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        null,
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                active
        );
    }

    private UpdateTrainingPlanRequest updateTemplateRequest(String name, boolean active) {
        return new UpdateTrainingPlanRequest(
                name,
                "Upper body",
                TrainingPlanType.TEMPLATE,
                null,
                List.of(new TrainingPlanEntryRequest(
                        "exercise-1",
                        1,
                        null,
                        List.of(new PlannedSetRequest(1, 10, 60.0, null, null, 90, PlannedSetType.NORMAL))
                )),
                active
        );
    }

    private TrainingPlanDocument trainingPlan(String id, String name, boolean active) {
        return trainingPlan(id, name, TrainingPlanType.TEMPLATE, null, null, active);
    }

    private TrainingPlanDocument trainingPlan(String id,
                                              String name,
                                              TrainingPlanType type,
                                              LocalDate plannedDate,
                                              boolean active) {
        return trainingPlan(
                id,
                name,
                type,
                plannedDate,
                type == TrainingPlanType.PLANNED_WORKOUT ? TrainingPlanStatus.PLANNED : null,
                active
        );
    }

    private TrainingPlanDocument trainingPlan(String id,
                                              String name,
                                              TrainingPlanType type,
                                              LocalDate plannedDate,
                                              TrainingPlanStatus status,
                                              boolean active) {
        return TrainingPlanDocument.builder()
                .id(id)
                .userId("user-1")
                .name(name)
                .description("Upper body")
                .type(type)
                .status(status)
                .plannedDate(plannedDate)
                .entries(List.of(TrainingPlanEntry.builder()
                        .exerciseId("exercise-1")
                        .exerciseNameSnapshot("Bench Press")
                        .order(1)
                        .plannedSets(List.of(PlannedSet.builder()
                                .setNumber(1)
                                .reps(10)
                                .weight(60.0)
                                .restSeconds(90)
                                .type(PlannedSetType.NORMAL)
                                .build()))
                        .build()))
                .active(active)
                .createdAt(Instant.parse("2026-04-07T12:00:00Z"))
                .updatedAt(Instant.parse("2026-04-08T12:00:00Z"))
                .build();
    }
}
