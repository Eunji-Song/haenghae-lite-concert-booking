package kr.hhplus.be.server.api.layered.controller.queue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.api.layered.dto.queue.QueueTokenRequest;
import kr.hhplus.be.server.api.layered.dto.queue.QueueTokenResponse;
import kr.hhplus.be.server.api.layered.service.queue.QueueService;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Tag(name = "대기열")
public class QueueController {

    private final QueueService queueService;

    @Operation(summary = "대기열 토큰 발급")
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> issue(@CurrentUserUuid String userUuid,
                                                    @RequestBody @Valid QueueTokenRequest request) {
        return ResponseEntity.ok(queueService.enterQueue(userUuid, request));
    }

    @Operation(summary = "대기 상태 폴링 (활성 대기)")
    @GetMapping("/me")
    public ResponseEntity<QueueTokenResponse> me(@CurrentUserUuid String userUuid,
                                                  @RequestHeader("X-Queue-Token") String queueToken) {
        return ResponseEntity.ok(queueService.getQueueStatus(userUuid, queueToken));
    }
}