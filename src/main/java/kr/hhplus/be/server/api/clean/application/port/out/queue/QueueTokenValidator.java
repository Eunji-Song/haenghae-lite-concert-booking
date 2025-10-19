package kr.hhplus.be.server.api.clean.application.port.out.queue;

public interface QueueTokenValidator {
    void validateActiveToken(String queueToken, String userUuid, Long concertId);
}