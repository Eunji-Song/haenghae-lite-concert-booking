package kr.hhplus.be.server.api.clean.application.port.in.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull Long reservationId,
        @NotNull @Min(1) Long amount
) {

}
