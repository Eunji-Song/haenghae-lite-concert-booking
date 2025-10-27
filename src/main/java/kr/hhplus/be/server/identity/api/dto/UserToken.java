package kr.hhplus.be.server.identity.api.dto;

public record UserToken(
        String accessToken,
        Integer accessTokenExpiresIn,
        String refreshToken,
        Integer refreshTokenExpiresIn
) {}
