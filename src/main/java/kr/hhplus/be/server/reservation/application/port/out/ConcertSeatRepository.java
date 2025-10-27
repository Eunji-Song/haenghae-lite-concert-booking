package kr.hhplus.be.server.reservation.application.port.out;

public interface ConcertSeatRepository {
    boolean hold(Long concertId, String date, Long seatNo, String userUuid, long ttlSeconds);
    void release(Long concertId, String date, Long seatNo, String userUuid);
}