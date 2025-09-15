package kr.hhplus.be.server.api.layered.dto.concert;

import java.time.LocalDate;

public record OpenDateResponse(Long concertDateId, Long concertId, LocalDate date, boolean open) {
}
