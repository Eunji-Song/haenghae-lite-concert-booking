package kr.hhplus.be.server.api.clean.interfaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.clean.application.port.in.payment.PaymentRequest;
import kr.hhplus.be.server.api.clean.application.port.out.queue.QueueTokenValidator;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kr.hhplus.be.server.api.clean.application.port.in.payment.PayForReservationCommand;
import kr.hhplus.be.server.api.clean.application.port.in.payment.PaymentResponse;
import kr.hhplus.be.server.api.clean.application.usecase.PayForReservationUseCase;
import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import org.springframework.validation.annotation.Validated;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Validated
@Tag(name = "결제")
public class PaymentController {

    private final PayForReservationUseCase payForReservationUseCase;
    private final QueueTokenValidator queueTokenValidator;

    // (임시) 큐 토큰 유효성 검증을 위해 예약→콘서트 식별 필요
    private final ReservationRepository reservationRepository;

    /**
     * 결제 & 예약 확정
     * Headers:
     * - Authorization: Bearer <Access JWT>
     * - X-Queue-Token: <Queue JWT>
     * - Idempotency-Key: <UUID>
     */
    @Operation(summary = "결제 & 예약 확정")
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
            @CurrentUserUuid String userUuid,
            @RequestHeader("X-Queue-Token") String queueToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + request.reservationId()));
        Long pseudoConcertId = reservation.getConcertDateId(); // TODO: 실제로는 concertDateId → concertId 매핑 필요

        // 큐 토큰이 ACTIVE인지 검증 (미ACTIVE/만료 시 예외)
        queueTokenValidator.validateActiveToken(queueToken, userUuid, pseudoConcertId);

        // 결제 유스케이스 실행
        PaymentResponse response =
                payForReservationUseCase.payForReservation(
                        new PayForReservationCommand(request.reservationId(), request.amount(), idempotencyKey)
                );

        return ResponseEntity.ok(response);
    }

}