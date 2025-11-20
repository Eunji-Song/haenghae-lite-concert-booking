package kr.hhplus.be.server.product.infrastructure.jpa.repository;

import kr.hhplus.be.server.product.infrastructure.jpa.entity.ConcertDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertDateJpaRepository extends JpaRepository<ConcertDateEntity, Long> {

    /**
     * 공연 ID와 날짜로 단일 공연일자를 조회합니다.
     * (UNIQUE (concert_id, event_date) 조건으로 항상 단일 레코드만 존재)
     *
     * @param concertId 조회할 공연의 ID
     * @param eventDate 조회할 공연 날짜
     * @return 해당 날짜의 공연 엔티티 (없으면 Optional.empty)
     */
    Optional<ConcertDateEntity> findByConcert_IdAndEventDate(Long concertId, LocalDate eventDate);

    /**
     * 특정 공연의 '예매 오픈 상태(is_open=true)'인 날짜 목록을
     * 날짜(eventDate) 기준으로 오름차순 정렬하여 조회합니다.
     *
     * @param concertId 공연 ID
     * @return 오픈된 날짜의 ConcertDateEntity 리스트
     */
    List<ConcertDateEntity> findAllByConcert_IdAndOpenTrueOrderByEventDateAsc(Long concertId);

    /**
     * 특정 공연의 특정 날짜가 존재하는지 여부를 확인합니다.
     * (즉, concert_id와 event_date 조합이 UNIQUE 키로 존재하는지 체크)
     *
     * @param concertId 공연 ID
     * @param eventDate 공연 날짜
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByConcert_IdAndEventDate(Long concertId, LocalDate eventDate);

    /**
     * 특정 공연의 모든 날짜를 조회합니다.
     * (is_open 여부와 상관없이 날짜 오름차순으로 정렬)
     *
     * @param concertId 공연 ID
     * @return 해당 공연의 모든 날짜 목록
     */
    List<ConcertDateEntity> findAllByConcert_IdOrderByEventDateAsc(Long concertId);

    /**
     * 공연 ID와 날짜로 concert_dates 테이블의 PK(id)를 조회합니다.
     * SELECT 절에서 id만 반환하므로, 엔티티 전체를 불러오지 않고 식별자만 얻을 때 효율적입니다.
     *
     * @param concertId 공연 ID
     * @param eventDate 공연 날짜
     * @return 해당 (concertId, eventDate)에 해당하는 id (없으면 Optional.empty)
     */
    @Query("SELECT cd.id " +
            "FROM ConcertDateEntity cd " +
            "WHERE cd.concert.id = :concertId AND cd.eventDate = :eventDate")
    Optional<Long> findIdByConcertIdAndEventDate(@Param("concertId") Long concertId,
                                                 @Param("eventDate") LocalDate eventDate);

    /**
     * concert_date의 ID로 조회하면서, concert 엔티티를 즉시 로딩(fetch join)하여 함께 조회합니다.
     * → concert 필드가 LAZY 로딩으로 설정된 경우, N+1 문제를 방지할 수 있습니다.
     *
     * @param id concert_dates 테이블의 PK
     * @return ConcertDateEntity (concert 엔티티 포함)
     */
    @Query("SELECT cd FROM ConcertDateEntity cd JOIN FETCH cd.concert WHERE cd.id = :id")
    Optional<ConcertDateEntity> findWithConcert(@Param("id") Long id);
}