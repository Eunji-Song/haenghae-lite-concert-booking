package kr.hhplus.be.server.common.fixture;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.entity.ReservationEntity;

import java.util.UUID;

public final class Payments {
    private Payments(){}

    public static PaymentEntity pending(ReservationEntity reservation, UserEntity user, long amount) {
        return PaymentEntity.builder()
                .reservation(reservation)
                .user(user)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentEntity succeeded(ReservationEntity reservation, UserEntity user, long amount) {
        return PaymentEntity.builder()
                .reservation(reservation)
                .user(user)
                .amount(amount)
                .status(PaymentStatus.SUCCEEDED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }
}