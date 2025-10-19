package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;

import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.common.entity.concert.ConcertDateEntity;
import kr.hhplus.be.server.common.entity.concert.ConcertSeatEntity;
import org.springframework.stereotype.Component;

/**
 * Reservation 도메인과 ReservationEntity 간 변환
 * Builder 패턴을 사용하여 안전하게 매핑
 */
@Component
public class ReservationMapper {

    /**
     * 도메인 → 엔티티 변환
     */
    public ReservationEntity toEntity(Reservation domain) {
        if (domain == null) {
            return null;
        }

        // ReservationEntity에 Builder 추가 필요
        ReservationEntity.ReservationEntityBuilder builder = ReservationEntity.builder()
                .user(createUserReference(domain.getUserId()))
                .concertDate(createConcertDateReference(domain.getConcertDateId()))
                .concertSeats(createConcertSeatReference(domain.getSeatId()))
                .status(domain.getStatus())
                .amount(domain.getAmount())
                .holdExpiresAt(domain.getHoldExpiresAt())
                .confirmedAt(domain.getConfirmedAt())
                .canceledAt(domain.getCanceledAt())
                .version(domain.getVersion());

        // ID가 있으면 설정 (업데이트 시)
        if (domain.getId() != null) {
            builder.id(domain.getId());
        }
        return builder.build();
    }

    /**
     * 엔티티 → 도메인 변환
     */
    public Reservation toDomain(ReservationEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Reservation(
                entity.getId(),
                entity.getUser().getId(),
                entity.getConcertDate().getId(),
                entity.getConcertSeats().getId(),
                entity.getStatus(),
                entity.getAmount(),
                entity.getHoldExpiresAt(),
                entity.getConfirmedAt(),
                entity.getCanceledAt(),
                entity.getVersion()
        );
    }

    // === 연관 엔티티 참조 생성 헬퍼 메서드들 ===

    /**
     * User 엔티티 참조 생성 (ID만 설정)
     */
    private UserEntity createUserReference(Long userId) {
        if (userId == null) {
            return null;
        }

        return UserEntity.builder()
                .id(userId)
                .build();
    }

    /**
     * ConcertDate 엔티티 참조 생성 (ID만 설정)
     */
    private ConcertDateEntity createConcertDateReference(Long concertDateId) {
        if (concertDateId == null) {
            return null;
        }

        return ConcertDateEntity.builder()
                .id(concertDateId)
                .build();
    }

    /**
     * ConcertSeat 엔티티 참조 생성 (ID만 설정)
     */
    private ConcertSeatEntity createConcertSeatReference(Long seatId) {
        if (seatId == null) {
            return null;
        }

        return ConcertSeatEntity.builder()
                .id(seatId)
                .build();
    }
}