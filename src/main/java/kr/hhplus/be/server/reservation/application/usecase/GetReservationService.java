package kr.hhplus.be.server.reservation.application.usecase;

import kr.hhplus.be.server.common.exception.reservation.ReservationAccessDeniedException;
import kr.hhplus.be.server.common.exception.reservation.ReservationNotFoundException;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import kr.hhplus.be.server.reservation.application.port.in.usecase.GetReservationUseCase;
import kr.hhplus.be.server.reservation.application.port.in.result.ReservationDetailResponse;
import kr.hhplus.be.server.reservation.application.port.out.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetReservationService implements GetReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final UserJpaRepository userRepo;

    @Override
    public ReservationDetailResponse get(String userUuid, Long reservationId) {
        var userId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);

        var r = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationNotFoundException::new);
        if (!r.getUserId().equals(userId)) throw new ReservationAccessDeniedException();

        return new ReservationDetailResponse(
                r.getId(), r.getConcertId(), // concert
                null, // date 문자열 변환은 product와의 조합이 필요. 간단화.
                null, // seatNo 역시 product 테이블 필요. 간단화.
                r.getStatus().name(),
                r.getAmount(),
                r.getHoldExpiresAt(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    @Override
    public List<ReservationDetailResponse> getMy(String userUuid) {
        var userId = userRepo.findIdByUserUuid(userUuid)
                .orElseThrow(ReservationAccessDeniedException::new);
        return reservationRepository.findByUserId(userId).stream()
                .map(r -> new ReservationDetailResponse(
                        r.getId(), r.getConcertId(), null, null,
                        r.getStatus().name(), r.getAmount(),
                        r.getHoldExpiresAt(), r.getCreatedAt(), r.getUpdatedAt()
                ))
                .toList();
    }
}