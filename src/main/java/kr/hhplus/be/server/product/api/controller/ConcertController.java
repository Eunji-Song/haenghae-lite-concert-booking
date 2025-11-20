package kr.hhplus.be.server.product.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import kr.hhplus.be.server.product.api.dto.OpenDateResponse;
import kr.hhplus.be.server.product.api.dto.SeatAvailabilityResponse;
import kr.hhplus.be.server.product.application.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "콘서트 일정 및 좌석")
@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @Operation(summary = "콘서트 예약 가능 일자 조회")
    @GetMapping("/{concertId}/dates")
    public ResponseEntity<List<OpenDateResponse>> getOpenDates(
            @Parameter(hidden = true)  @CurrentUserUuid String userUuid,
            @RequestHeader("X-Queue-Token") String queueToken,
            @PathVariable Long concertId
    ) {
        return ResponseEntity.ok(concertService.getOpenDates(concertId));
    }

    @Operation(summary = "콘서트 예약 가능 일자별 좌석 조회")
    @GetMapping("/{concertId}/dates/{date}/seats")
    public ResponseEntity<List<SeatAvailabilityResponse>> getSeats(
            @Parameter(hidden = true) @CurrentUserUuid String userUuid,
            @RequestHeader("X-Queue-Token") String queueToken,
            @PathVariable Long concertId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(concertService.getSeats(concertId, date));
    }
}