package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.service;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.controller.UpdateExerciseRequest;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.model.ExerciseDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.exercise.repository.ExerciseRepository;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserProvider;
import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=true")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ExerciseMongoIntegrationTest {

    private static final String USER_EMAIL = "user@email.com";

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Autowired
    ExerciseService exerciseService;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Autowired
    UserRepository userRepository;

    @DynamicPropertySource
    static void configureMongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @BeforeEach
    void setUp() {
        exerciseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should persist exercise update in Mongo")
    void updateExercisePersistsChangesInMongo() {
        UserDocument user = userRepository.save(user());
        ExerciseDocument exercise = exerciseRepository.save(ExerciseDocument.builder()
                .userId(user.getId())
                .name("Bench Press")
                .category("CHEST")
                .build());

        ExerciseDocument updated = exerciseService.updateExercise(
                USER_EMAIL,
                exercise.getId(),
                new UpdateExerciseRequest(" Incline Bench Press ", "UPPER CHEST", " Upper chest pressing ")
        );

        ExerciseDocument reloaded = exerciseRepository.findById(exercise.getId()).orElseThrow();
        assertThat(updated.getId()).isEqualTo(exercise.getId());
        assertThat(reloaded.getName()).isEqualTo("Incline Bench Press");
        assertThat(reloaded.getNameNormalized()).isEqualTo("incline bench press");
        assertThat(reloaded.getCategory()).isEqualTo("UPPER CHEST");
        assertThat(reloaded.getDescription()).isEqualTo("Upper chest pressing");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should enforce unique user name index in Mongo")
    void uniqueIndexPreventsDuplicateNormalizedNames() {
        UserDocument user = userRepository.save(user());
        exerciseRepository.save(ExerciseDocument.builder()
                .userId(user.getId())
                .name("Bench Press")
                .category("CHEST")
                .build());
        ExerciseDocument otherExercise = exerciseRepository.save(ExerciseDocument.builder()
                .userId(user.getId())
                .name("Squat")
                .category("LEGS")
                .build());

        otherExercise.setName("  BENCH PRESS  ");

        assertThatThrownBy(() -> exerciseRepository.save(otherExercise))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private UserDocument user() {
        return UserDocument.builder()
                .email(USER_EMAIL)
                .provider(UserProvider.LOCAL)
                .roles(List.of("ROLE_USER"))
                .build();
    }
}
