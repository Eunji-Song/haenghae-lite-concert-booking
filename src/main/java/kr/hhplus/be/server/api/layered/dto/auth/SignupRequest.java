package kr.hhplus.be.server.api.layered.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String password;
    private String name;
}
