package kr.hhplus.be.server.api.layered.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserToken {
    String accessToken;
    Integer accessTokenExpiresIn;
    String refreshToken;
    Integer refreshTokenExpiresIn;
}