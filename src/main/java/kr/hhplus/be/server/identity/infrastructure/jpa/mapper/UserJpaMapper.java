package kr.hhplus.be.server.identity.infrastructure.jpa.mapper;

import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserJpaMapper {

    public static User toDomain(UserEntity e) {
        if (e == null) return null;
        return new User(
                e.getId(),
                e.getUserUuid(),
                e.getEmail(),
                e.getName(),
                e.getPassword()
        );
    }

    public static UserEntity toEntity(User d) {
        if (d == null) return null;
        return UserEntity.builder()
                .id(d.id())
                .userUuid(d.userUuid())
                .email(d.email())
                .name(d.name())
                .password(d.passwordHash())
                .build();
    }
}