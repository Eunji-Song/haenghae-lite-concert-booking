package kr.hhplus.be.server.identity.infrastructure.jpa.adapter;

import kr.hhplus.be.server.identity.domain.repository.UserTokenRepository;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserTokenEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTokenRepositoryAdapter implements UserTokenRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserTokenJpaRepository userTokenJpaRepository;

    @Transactional
    @Override
    public void replace(String userUuid, String refreshToken, LocalDateTime expiresAt) {
        // 기존 토큰 제거
        userTokenJpaRepository.deleteByUserUserUuid(userUuid);

        // 새 토큰 저장
        var user = userJpaRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new IllegalStateException("User not found for token save"));

        var token = UserTokenEntity.builder()
                .user(user)
                .refreshTokenHash(refreshToken)
                .expiresAt(expiresAt)
                .build();

        userTokenJpaRepository.save(token);
    }

    @Override
    public Optional<String> findByUserUuid(String userUuid) {
        return userTokenJpaRepository.findByUserUserUuid(userUuid)
                .map(UserTokenEntity::getRefreshTokenHash);
    }
}