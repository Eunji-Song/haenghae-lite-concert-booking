package kr.hhplus.be.server.api.layered.infrastructure.queue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import kr.hhplus.be.server.api.clean.application.port.out.queue.QueueTokenValidator;
import kr.hhplus.be.server.api.layered.service.user.UserService;
import kr.hhplus.be.server.common.exception.queue.InvalidQueueTokenException;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryQueueTokenValidator implements QueueTokenValidator {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;                 // uuid→UserEntity(id) 조회용
    private final InMemoryQueueManager inMemoryQueueManager;

    @Override
    public void validateActiveToken(String queueToken, String userUuid, Long concertId) {
        try {
            // 1) 토큰 파싱 (서명/만료 검증 포함)
            Jws<Claims> jws = jwtTokenProvider.parseQueue(queueToken);
            Claims body = jws.getBody();

            // 2) 주체(uuid) 및 concertId 일치 여부 확인
            String subjectUuid = body.getSubject();
            Object claimConcertId = body.get("concertId");
            Long tokenConcertId = (claimConcertId instanceof Number)
                    ? ((Number) claimConcertId).longValue()
                    : Long.valueOf(claimConcertId.toString());

            if (!userUuid.equals(subjectUuid) || !concertId.equals(tokenConcertId)) {
                throw new InvalidQueueTokenException(); // 다른 유저/다른 콘서트용 토큰
            }

            // 3) ACTIVE 상태인지 확인
            var user = userService.getUser(userUuid); // <- uuid로 유저 조회(id 얻기)
            boolean active = inMemoryQueueManager.isActiveUser(user.getId(), concertId);
            if (!active) {
                throw new InvalidQueueTokenException(); // 아직 대기중(ISSUED) 혹은 만료
            }

        } catch (InvalidQueueTokenException e) {
            throw e;
        } catch (Exception e) {
            // 파싱 실패/클레임 이상 등
            throw new InvalidQueueTokenException();
        }
    }
}