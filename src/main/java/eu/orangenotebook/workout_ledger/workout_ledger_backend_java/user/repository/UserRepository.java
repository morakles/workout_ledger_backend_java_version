package eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.repository;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.user.model.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByEmail(String email);

    boolean existsByEmail(String email);
}
