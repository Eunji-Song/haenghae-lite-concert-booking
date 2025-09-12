package kr.hhplus.be.server.common.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey accessKey;
    private final long accessExpiry;
    private final SecretKey refreshKey;
    private final long refreshExpiry;
    private final SecretKey queueKey;
    private final long queueExpiry;

    public JwtTokenProvider(JwtProperties props) {
        this.accessKey = Keys.hmacShaKeyFor(props.getAccess().getSecret().getBytes());
        this.accessExpiry = props.getAccess().getExpirySeconds();
        this.refreshKey = Keys.hmacShaKeyFor(props.getRefresh().getSecret().getBytes());
        this.refreshExpiry = props.getRefresh().getExpirySeconds();
        this.queueKey = Keys.hmacShaKeyFor(props.getQueue().getSecret().getBytes());
        this.queueExpiry = props.getQueue().getExpirySeconds();
    }

    public String createAccessToken(String userUuid, Map<String, Object> claims) {
        Map<String, Object> tokenClaims = claims != null ? claims : Map.of();
        return create(userUuid, tokenClaims, accessKey, accessExpiry);
    }

    public String createRefreshToken(String userUuid) {
        return create(userUuid, Map.of("typ", "refresh"), refreshKey, refreshExpiry);
    }

    public String createQueueToken(String userUuid, Long concertId, Map<String, Object> queueClaims) {
        var claims = new java.util.HashMap<String, Object>(queueClaims);
        claims.put("typ", "queue");
        claims.put("concertId", concertId);
        return create(userUuid, claims, queueKey, queueExpiry);
    }

    private String create(String subject, Map<String, Object> claims, SecretKey key, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 테스트에서 사용할 만료 시간 getter 추가
    public Integer getAccessTokenExpiresIn() {
        return (int) accessExpiry;
    }

    public Integer getRefreshTokenExpiresIn() {
        return (int) refreshExpiry;
    }

    public Integer getQueueTokenExpiresIn() {
        return (int) queueExpiry;
    }

    // 토큰 파싱 메서드들
    public Jws<Claims> parseAccess(String token) {
        return parse(token, accessKey);
    }

    public Jws<Claims> parseRefresh(String token) {
        return parse(token, refreshKey);
    }

    public Jws<Claims> parseQueue(String token) {
        return parse(token, queueKey);
    }

    private Jws<Claims> parse(String token, SecretKey key) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}