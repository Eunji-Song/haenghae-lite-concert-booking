package kr.hhplus.be.server.identity.api.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import kr.hhplus.be.server.identity.api.dto.UserResponse;
import kr.hhplus.be.server.identity.application.service.UserService;
import kr.hhplus.be.server.identity.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 조회(by UUID)")
    @GetMapping("/{userUuid}")
    public ResponseEntity<UserResponse> getUser(@PathVariable @NotBlank String userUuid) {
        User user = userService.getUser(userUuid);
        return ResponseEntity.ok(UserResponse.of(user.userUuid(), user.email(), user.name()));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@Parameter(hidden = true) @CurrentUserUuid String userUuid) {
        User user = userService.getUser(userUuid);
        return ResponseEntity.ok(UserResponse.of(user.userUuid(), user.email(), user.name()));
    }
}