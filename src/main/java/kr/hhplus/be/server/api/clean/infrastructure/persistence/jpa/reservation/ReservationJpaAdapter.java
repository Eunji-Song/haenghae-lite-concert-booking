package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.reservation;


import kr.hhplus.be.server.api.clean.application.port.out.reservation.ReservationRepository;
import kr.hhplus.be.server.api.clean.domain.model.reservation.Reservation;
import kr.hhplus.be.server.common.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ReservationRepository의 JPA 구현체
 * 도메인 Repository 인터페이스를 JPA Entity와 연결
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationJpaAdapter implements ReservationRepository {

    private final ReservationEntityRepository entityRepository;
    private final ReservationMapper mapper;

    @Override
    public Reservation save(Reservation reservation) {
        log.debug("예약 저장 - reservationId: {}", reservation.getId());

        // 도메인 → 엔티티 변환
        ReservationEntity entity = mapper.toEntity(reservation);

        // JPA 저장
        ReservationEntity savedEntity = entityRepository.save(entity);

        // 엔티티 → 도메인 변환
        Reservation savedReservation = mapper.toDomain(savedEntity);

        log.debug("예약 저장 완료 - reservationId: {}", savedReservation.getId());
        return savedReservation;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        log.debug("예약 조회 - reservationId: {}", id);

        return entityRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveBySeat(Long seatId) {
        log.debug("좌석 활성 예약 존재 여부 확인 - seatId: {}", seatId);

        // PENDING 또는 CONFIRMED 상태인 예약이 있는지 확인
        return entityRepository.existsActiveBySeatId(seatId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
    }

    @Override
    public List<Reservation> findByUserId(Long userId) {
        log.debug("사용자 예약 목록 조회 - userId: {}", userId);

        List<ReservationEntity> entities = entityRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Reservation> findActiveBySeat(Long seatId) {
        log.debug("좌석별 활성 예약 조회 - seatId: {}", seatId);

        List<ReservationEntity> entities = entityRepository.findActiveBySeatId(seatId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}