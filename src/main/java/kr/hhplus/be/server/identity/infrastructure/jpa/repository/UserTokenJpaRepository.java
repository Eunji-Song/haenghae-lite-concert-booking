package kr.hhplus.be.server.identity.infrastructure.jpa.repository;

import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenJpaRepository extends JpaRepository<UserTokenEntity, Long> {
    Optional<UserTokenEntity> findByUserUserUuid(String userUuid);
    void deleteByUserUserUuid(String userUuid);
}