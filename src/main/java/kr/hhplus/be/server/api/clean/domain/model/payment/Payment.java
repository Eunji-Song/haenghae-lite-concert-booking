package kr.hhplus.be.server.api.clean.domain.model.payment;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Payment {
    private final Long id;
    private final Long reservationId;
    private final Long amount;
    private final String provider;
    private final String providerTxnId;
    private final PaymentStatus status;
    private final LocalDateTime succeededAt;
    private final LocalDateTime failedAt;
    private final String idempotencyKey;
}