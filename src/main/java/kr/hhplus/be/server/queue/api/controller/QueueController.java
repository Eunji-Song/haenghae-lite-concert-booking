package kr.hhplus.be.server.queue.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import kr.hhplus.be.server.queue.api.dto.QueueTokenRequest;
import kr.hhplus.be.server.queue.api.dto.QueueTokenResponse;
import kr.hhplus.be.server.queue.application.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "대기열")
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @Operation(summary = "대기열 토큰 발급")
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> issue(
            @CurrentUserUuid String userUuid,
            @RequestBody QueueTokenRequest request
    ) {
        var entry = queueService.issue(userUuid, request.concertId());
        // ETA 계산 로직이 있으면 여기에 넣어주세요 (null 유지 가능)
        Long eta = null;
        return ResponseEntity.ok(QueueTokenResponse.issued(entry.token(), entry.rank(), eta));
    }

    @Operation(summary = "대기 상태 폴링 (활성 대기)")
    @GetMapping("/me")
    public ResponseEntity<QueueTokenResponse> me(
            @CurrentUserUuid String userUuid,
            @RequestHeader("X-Queue-Token") String queueToken
    ) {
        QueueStatus status = queueService.statusOf(queueToken, userUuid);

        return switch (status) {
            case ACTIVE   -> ResponseEntity.ok(QueueTokenResponse.active(queueToken));
            case EXPIRED  -> ResponseEntity.ok(QueueTokenResponse.expired(queueToken));
            case ISSUED   -> {
                var rank = queueService.rankOf(queueToken).orElse(null);
                Long eta = null; // 필요시 추정치 계산
                yield ResponseEntity.ok(QueueTokenResponse.issued(queueToken, rank, eta));
            }
        };
    }
}