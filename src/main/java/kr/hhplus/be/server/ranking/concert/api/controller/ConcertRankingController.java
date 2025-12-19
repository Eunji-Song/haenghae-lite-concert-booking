package kr.hhplus.be.server.ranking.concert.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.ranking.concert.api.dto.FastSoldOutConcertResponse;
import kr.hhplus.be.server.ranking.concert.application.service.ConcertRankingService;
import kr.hhplus.be.server.ranking.concert.domain.model.FastSoldOutConcert;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 콘서트 빠른 매진 랭킹 조회용 API 컨트롤러.
 *
 * - 월별 "빠른 매진 랭킹" 조회
 * - 예: GET /api/v1/rankings/concerts/soldout?month=2025-12&limit=10
 */
@Tag(name = "콘서트 랭킹", description = "빠른 매진 랭킹 조회 API")
@RestController
@RequestMapping("/api/v1/rankings/concerts")
@RequiredArgsConstructor
public class ConcertRankingController {

    private final ConcertRankingService concertRankingService;
    private final Clock clock; 

    @Operation(
            summary = "월별 빠른 매진 콘서트 랭킹 조회",
            description = """
                    주어진 month(yyyy-MM) 기준으로, 가장 빨리 매진된 콘서트 TOP N을 조회합니다.
                    - Redis Sorted Set을 이용해 매진 시각(soldOutAt)을 score로 저장합니다.
                    - score가 작을수록(더 이른 시각일수록) 높은 순위(1등)에 위치합니다.
                    - month 파라미터가 비어있으면, 서버 기준 현재 월(예: Asia/Seoul)을 기본값으로 사용합니다.
                    """
    )
    @GetMapping("/soldout")
    public ResponseEntity<List<FastSoldOutConcertResponse>> getFastSoldOutRanking(
            @Parameter(description = "조회 대상 월 (yyyy-MM). 예: 2025-12. 비워두면 현재 월 기준.")
            @RequestParam(name = "month", required = false) String monthParam,

            @Parameter(description = "조회할 랭킹 개수 (기본값 10)")
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        if (limit <= 0) return ResponseEntity.ok(List.of()); 

        YearMonth month = resolveMonthOrDefault(monthParam);
        List<FastSoldOutConcert> topN = concertRankingService.getFastSoldOutTopN(month, limit);

        // rank는 1부터 시작
        List<FastSoldOutConcertResponse> body = buildRankingResponse(topN);

        return ResponseEntity.ok(body);
    }

    /**
     * month 쿼리 파라미터를 YearMonth로 변환.
     * - null/blank → 현재 월(Clock 기준, KST) 사용
     * - 형식이 잘못된 경우 400 Bad Request
     */
    private YearMonth resolveMonthOrDefault(String monthParam) {
        if (monthParam == null || monthParam.isBlank()) {
            return YearMonth.now(clock); 
        }
        
        try {
            return YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "month 파라미터는 yyyy-MM 형식이어야 합니다. 예: 2025-12",
                    e
            );
        }
    }

    private List<FastSoldOutConcertResponse> buildRankingResponse(List<FastSoldOutConcert> topN) {
        return topN.stream()
                .map(fastSoldOut -> {
                    int rank = topN.indexOf(fastSoldOut) + 1; // 1-based rank
                    return new FastSoldOutConcertResponse(
                            rank,
                            fastSoldOut.concertId(),
                            fastSoldOut.soldOutAt()
                    );
                })
                .toList();
    }
}