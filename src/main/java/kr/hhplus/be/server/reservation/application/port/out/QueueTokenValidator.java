package kr.hhplus.be.server.reservation.application.port.out;

public interface QueueTokenValidator {
    boolean validate(String userUuid, Long concertId, String queueToken);
}