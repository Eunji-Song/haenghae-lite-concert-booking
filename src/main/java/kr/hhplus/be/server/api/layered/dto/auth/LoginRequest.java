package kr.hhplus.be.server.api.layered.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest {
    String email;
    String password;
}