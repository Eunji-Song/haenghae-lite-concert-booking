package kr.hhplus.be.server.common.fixture;

import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;

import java.util.UUID;

/**
 * 테스트 전용 Users Fixture
 * - 도메인 객체(User)
 * - JPA 엔티티(UserEntity)
 */
public final class Users {

    private Users() {} // 인스턴스화 방지

    // ----------------------------------------------------------------
    // ✅ [도메인 모델 생성] (kr.hhplus.be.server.identity.domain.model.User)
    // ----------------------------------------------------------------
    public static User domain() {
        return new User(
                fixedUuid(),
                "user@test.com",
                "테스트유저",
                "$2a$10$hashedPassword"
        );
    }

    public static User domainWith(String uuid, String email) {
        return new User(
                uuid,
                email,
                "사용자",
                "$2a$10$hashedPassword"
        );
    }

    // ----------------------------------------------------------------
    // ✅ [엔티티 생성] (kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity)
    // ----------------------------------------------------------------
    public static UserEntity entity() {
        return UserEntity.builder()
                .userUuid(fixedUuid())
                .email("entity@test.com")
                .password("$2a$10$hashedPassword")
                .name("엔티티유저")
                .build();
    }

    public static UserEntity entityWith(String email) {
        return UserEntity.builder()
                .userUuid(UUID.randomUUID().toString())
                .email(email)
                .password("$2a$10$hashedPassword")
                .name("테스트유저")
                .build();
    }

    public static UserEntity entityWithUuid(String userUuid) {
        return UserEntity.builder()
                .userUuid(userUuid)
                .email("uuid@test.com")
                .password("$2a$10$hashedPassword")
                .name("UUID테스트유저")
                .build();
    }

    // ----------------------------------------------------------------
    // ✅ [공용 유틸]
    // ----------------------------------------------------------------
    private static String fixedUuid() {
        return "11111111-1111-1111-1111-111111111111";
    }
}