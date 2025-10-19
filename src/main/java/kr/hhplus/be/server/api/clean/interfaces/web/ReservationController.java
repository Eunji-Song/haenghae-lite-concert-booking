package kr.hhplus.be.server.api.clean.interfaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.clean.application.port.out.queue.QueueTokenValidator;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kr.hhplus.be.server.api.clean.application.port.in.reservation.*;
import kr.hhplus.be.server.api.clean.application.usecase.*;
import kr.hhplus.be.server.api.layered.service.user.UserService;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
@Tag(name = "예약")
public class ReservationController {

    // Use Cases (클린)
    private final ReserveSeatUseCase reserveSeatUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final GetMyReservationsUseCase getMyReservationsUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final UserService userService;
    private final QueueTokenValidator queueTokenValidator;

    @Operation(summary = "좌석 예약(홀드)")
    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(
            @CurrentUserUuid String userUuid,
            @RequestHeader("X-Queue-Token") String queueToken,
            @Valid @RequestBody ReserveSeatRequest req
    ) {
        // 대기열 토큰: 활성 게이트 통과
        queueTokenValidator.validateActiveToken(queueToken, userUuid, req.concertId());

        Long userId = userService.getUser(userUuid).getId();

        var cmd = new ReserveSeatCommand(
                userId,
                req.concertId(),
                req.date(),
                req.seatNo()
        );

        var res = reserveSeatUseCase.reserveSeat(cmd);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + res.reservationId()))
                .body(res);
    }

    @Operation(summary = "예약 상세")
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationDetailResponse> getReservation(
            @CurrentUserUuid String userUuid,
            @PathVariable Long reservationId
    ) {
        Long userId = userService.getUser(userUuid).getId();

        var query = new GetReservationQuery(reservationId, userId);
        var res = getReservationUseCase.getReservation(query);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "내 예약 목록")
    @GetMapping("/me")
    public ResponseEntity<List<ReservationSummaryResponse>> getMyReservations(
            @CurrentUserUuid String userUuid
    ) {
        Long userId = userService.getUser(userUuid).getId();

        var query = new GetMyReservationsQuery(userId);
        var res = getMyReservationsUseCase.getMyReservations(query);
        return ResponseEntity.ok(res);
    }


    @Operation(summary = "예약 취소")
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<CancelReservationResponse> cancelReservation(
            @CurrentUserUuid String userUuid,
            @PathVariable Long reservationId
    ) {
        Long userId = userService.getUser(userUuid).getId();

        var cmd = new CancelReservationCommand(reservationId, userId);
        var res = cancelReservationUseCase.cancelReservation(cmd);
        return ResponseEntity.ok(res);
    }



}