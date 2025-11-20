package kr.hhplus.be.server.identity.domain.repository;

import kr.hhplus.be.server.identity.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(String userUuid);
    Optional<User> findById(Long id);
    User save(User user);
}