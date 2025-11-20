package kr.hhplus.be.server.reservation.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import kr.hhplus.be.server.reservation.api.dto.ReserveSeatRequest;
import kr.hhplus.be.server.reservation.application.port.in.command.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationDetailResponse;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationResponse;
import kr.hhplus.be.server.reservation.application.port.in.usecase.CancelReservationUseCase;
import kr.hhplus.be.server.reservation.application.port.in.usecase.GetReservationUseCase;
import kr.hhplus.be.server.reservation.application.port.in.usecase.ReserveSeatUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "예약")
public class ReservationController {

    private final ReserveSeatUseCase reserveSeatUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;

    @Operation(summary = "좌석 예약(홀드)")
    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(
            @CurrentUserUuid String userUuid,
            @RequestBody ReserveSeatRequest request,
            @RequestHeader("X-Queue-Token") String queueToken
    ) {
        var cmd = new ReserveSeatCommand(
                userUuid,
                request.concertId(),
                LocalDate.parse(request.date()),
                request.seatNo(),
                60L
        );
        return ResponseEntity.ok(reserveSeatUseCase.reserve(cmd));
    }

    @Operation(summary = "예약 취소")
    @DeleteMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancel(
            @CurrentUserUuid String userUuid,
            @PathVariable Long reservationId
    ) {
        cancelReservationUseCase.cancel(userUuid, reservationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "예약 상세")
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationDetailResponse> get(
            @CurrentUserUuid String userUuid,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(getReservationUseCase.get(userUuid, reservationId));
    }

    @Operation(summary = "내 예약 목록")
    @GetMapping()
    public ResponseEntity<java.util.List<ReservationDetailResponse>> getMy(
            @CurrentUserUuid String userUuid
    ) {
        return ResponseEntity.ok(getReservationUseCase.getMy(userUuid));
    }
}