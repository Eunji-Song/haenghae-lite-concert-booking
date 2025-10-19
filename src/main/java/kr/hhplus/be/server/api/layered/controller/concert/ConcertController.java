package kr.hhplus.be.server.api.layered.controller.concert;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.clean.application.port.out.queue.QueueTokenValidator;
import kr.hhplus.be.server.api.layered.dto.concert.OpenDateResponse;
import kr.hhplus.be.server.api.layered.dto.concert.SeatAvailabilityResponse;
import kr.hhplus.be.server.api.layered.service.concert.ConcertService;
import kr.hhplus.be.server.common.security.user.CurrentUserUuid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concerts")
@Tag(name = "콘서트 일정 및 좌석")
public class ConcertController {

    private final ConcertService concertService;
    private final QueueTokenValidator queueTokenValidator;

    @Operation(summary = "콘서트 예약 가능 일자 조회")
    @GetMapping("/{concertId}/dates")
    public List<OpenDateResponse> getOpenDates(@CurrentUserUuid String userUuid,
                                               @RequestHeader("X-Queue-Token") String queueToken,
                                               @PathVariable Long concertId) {
        queueTokenValidator.validateActiveToken(queueToken, userUuid, concertId);
        return concertService.getOpenDates(concertId);
    }

    @Operation(summary = "콘서트 예약 가능 일자별 좌석 조회")
    @GetMapping("/{concertId}/dates/{date}/seats")
    public List<SeatAvailabilityResponse> getSeats(@CurrentUserUuid String userUuid,
                                                   @RequestHeader("X-Queue-Token") String queueToken,
                                                   @PathVariable Long concertId,
                                                   @PathVariable String date) {
        queueTokenValidator.validateActiveToken(queueToken, userUuid, concertId);
        return concertService.getAvailableSeatList(concertId, date);
    }
}