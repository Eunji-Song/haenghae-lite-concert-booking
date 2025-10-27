package kr.hhplus.be.server.payment.application.port.out;


/**
 * 대기열 아웃바운드 포트.
 * 결제 서비스 → 대기열 모듈로의 의존성을 역전시키는 인터페이스.
 */
public interface QueuePort {

    /**
     * 결제 시도 전, 토큰이 해당 사용자/콘서트에 대해 유효/활성인지 검증.
     * 유효하지 않으면 예외를 던진다.
     */
    void validate(String userUuid, String queueToken);

    /**
     * 토큰 만료 처리(예: 결제 완료 후, 혹은 만료 정책에 따른 비활성화).
     */
    void expireToken(String userUuid, Long concertId);
}