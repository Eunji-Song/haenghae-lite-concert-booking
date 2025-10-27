package kr.hhplus.be.server.common.fixture;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;

import java.util.UUID;

public final class Queues {
    private Queues(){}

    public static QueueEntry issued(String userUuid, Long concertId, long rank) {
        return new QueueEntry(UUID.randomUUID().toString(), userUuid, concertId, rank, QueueStatus.ISSUED);
    }

    public static QueueEntry active(String userUuid, Long concertId) {
        return new QueueEntry(UUID.randomUUID().toString(), userUuid, concertId, 0L, QueueStatus.ACTIVE);
    }
}