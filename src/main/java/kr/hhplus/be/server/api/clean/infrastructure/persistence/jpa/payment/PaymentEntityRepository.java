package kr.hhplus.be.server.api.clean.infrastructure.persistence.jpa.payment;

import kr.hhplus.be.server.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * PaymentEntity JPA Repository
 */
public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    /**
     * 예약별 결제 목록 조회 (최신순)
     */
    List<PaymentEntity> findByReservationIdOrderByCreatedAtDesc(Long reservationId);

    /**
     * 멱등성 키로 결제 조회
     */
    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * 사용자별 결제 목록 조회 (예약을 통한 간접 조회)
     */
    @Query("""
        SELECT p 
        FROM PaymentEntity p 
        JOIN p.reservation r 
        WHERE r.user.id = :userId 
        AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
        """)
    List<PaymentEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 특정 상태의 결제 목록 조회
     */
    List<PaymentEntity> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    /**
     * 예약별 성공한 결제 조회
     */
    @Query("""
        SELECT p 
        FROM PaymentEntity p 
        WHERE p.reservation.id = :reservationId 
        AND p.status = 'SUCCEEDED'
        AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
        """)
    List<PaymentEntity> findSucceededPaymentsByReservationId(@Param("reservationId") Long reservationId);
}