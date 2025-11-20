package kr.hhplus.be.server.identity.infrastructure.jpa.repository;

import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUserUuid(String userUuid);
    boolean existsByEmail(String email);

    @Query("select u.id from UserEntity u where u.userUuid = :userUuid")
    Optional<Long> findIdByUserUuid(String userUuid);
}