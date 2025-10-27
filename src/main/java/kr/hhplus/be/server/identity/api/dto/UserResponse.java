package kr.hhplus.be.server.identity.api.dto;


import kr.hhplus.be.server.identity.domain.model.User;

public record UserResponse(
        String userUuid,
        String email,
        String name
) {
    public static UserResponse of(String userUuid, String email, String name) {
        return new UserResponse(userUuid, email, name);
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getUserUuid(), user.getEmail(), user.getName());
    }
}