package kr.hhplus.be.server.identity.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;



public interface UserTokenRepository {
    void replace(String userUuid, String refreshToken, LocalDateTime expiresAt);
    Optional<String> findByUserUuid(String userUuid);
}