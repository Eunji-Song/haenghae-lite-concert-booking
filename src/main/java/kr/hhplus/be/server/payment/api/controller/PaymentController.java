package kr.hhplus.be.server.payment.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import kr.hhplus.be.server.payment.api.dto.PaymentRequest;
import kr.hhplus.be.server.payment.application.port.in.command.PayForReservationCommand;
import kr.hhplus.be.server.payment.application.port.in.result.PaymentResponse;
import kr.hhplus.be.server.payment.application.port.in.usecase.PayForReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "결제")
public class PaymentController {

    private final PayForReservationUseCase payForReservationUseCase;

    @Operation(summary = "결제 & 예약 확정")
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
            @Parameter(hidden = true) @CurrentUserUuid String userUuid,
            @RequestBody PaymentRequest request,
            @RequestHeader("X-Queue-Token") String queueToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        var cmd = new PayForReservationCommand(
                userUuid, request.reservationId(), request.amount(), queueToken, idemKey);
        return ResponseEntity.ok(payForReservationUseCase.pay(cmd));
    }
}