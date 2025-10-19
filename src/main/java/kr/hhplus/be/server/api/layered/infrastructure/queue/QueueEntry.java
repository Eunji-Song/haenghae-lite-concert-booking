package kr.hhplus.be.server.api.layered.infrastructure.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 대기열 항목 클래스
 */

@Getter
@AllArgsConstructor
public class QueueEntry {
    private final Long userId;
    private final Long concertId;
    private final Long queueNumber;
    private final LocalDateTime enteredAt;
}
