package kr.hhplus.be.server.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 간단한 테스트용 시크릿 키 설정
        JwtProperties props = new JwtProperties();
        props.getAccess().setSecret("a1234567890a1234567890a1234567890a1234567890a1234567890a1234567890");
        props.getAccess().setExpirySeconds(3600);
        props.getRefresh().setSecret("r1234567890r1234567890r1234567890r1234567890r1234567890r1234567890");
        props.getRefresh().setExpirySeconds(86400);
        props.getQueue().setSecret("q1234567890q1234567890q1234567890q1234567890q1234567890q1234567890");
        props.getQueue().setExpirySeconds(600);

        jwtTokenProvider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("AccessToken 생성 및 파싱 성공")
    void createAndParseAccessToken() {
        // given
        String userUuid = "test-user-uuid";
        Map<String, Object> claims = Map.of("role", "USER");

        // when
        String token = jwtTokenProvider.createAccessToken(userUuid, claims);
        Jws<Claims> parsed = jwtTokenProvider.parseAccess(token);

        // then
        assertThat(parsed.getBody().getSubject()).isEqualTo(userUuid);
        assertThat(parsed.getBody().get("role")).isEqualTo("USER");
        assertThat(parsed.getBody().getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("RefreshToken 생성 및 파싱 성공")
    void createAndParseRefreshToken() {
        String userUuid = "test-user-uuid";
        String token = jwtTokenProvider.createRefreshToken(userUuid);
        Jws<Claims> parsed = jwtTokenProvider.parseRefresh(token);

        assertThat(parsed.getBody().getSubject()).isEqualTo(userUuid);
        assertThat(parsed.getBody().get("typ")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("QueueToken 생성 및 파싱 성공")
    void createAndParseQueueToken() {
        String userUuid = "queue-user-uuid";
        Long concertId = 123L;
        String token = jwtTokenProvider.createQueueToken(userUuid, concertId, Map.of("status", "WAIT"));

        Jws<Claims> parsed = jwtTokenProvider.parseQueue(token);

        assertThat(parsed.getBody().getSubject()).isEqualTo(userUuid);
        assertThat(parsed.getBody().get("concertId", Long.class)).isEqualTo(concertId);
        assertThat(parsed.getBody().get("status")).isEqualTo("WAIT");
        assertThat(parsed.getBody().get("typ")).isEqualTo("queue");
    }

    @Test
    @DisplayName("토큰 만료 시간 Getter 확인")
    void tokenExpiryGetters() {
        assertThat(jwtTokenProvider.getAccessTokenExpiresIn()).isEqualTo(3600);
        assertThat(jwtTokenProvider.getRefreshTokenExpiresIn()).isEqualTo(86400);
        assertThat(jwtTokenProvider.getQueueTokenExpiresIn()).isEqualTo(600);
    }
}