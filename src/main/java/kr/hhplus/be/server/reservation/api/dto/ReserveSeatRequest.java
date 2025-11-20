package kr.hhplus.be.server.reservation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @param concertId
 * @param date : yyyy-MM-dd
 * @param seatNo
 */
public record ReserveSeatRequest(Long concertId, String date, Integer seatNo) {
}