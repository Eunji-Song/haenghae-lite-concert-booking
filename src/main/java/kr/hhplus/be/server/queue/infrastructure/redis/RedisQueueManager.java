package kr.hhplus.be.server.queue.infrastructure.redis;

import kr.hhplus.be.server.common.enums.QueueStatus;
import kr.hhplus.be.server.queue.domain.model.QueueEntry;
import kr.hhplus.be.server.queue.domain.repository.QueueManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Profile("!test")
public class RedisQueueManager implements QueueManager {

    // 정책값
    private static final int ACTIVE_LIMIT = 50;
    private static final Duration ACTIVE_TTL = Duration.ofMinutes(10);
    private static final Duration WAITING_TTL = Duration.ofHours(1);

    // key prefix
    private static final String KEY_CONCERTS = "queue:concerts";
    private static final String KEY_ACTIVE_PREFIX = "queue:active:";     // ZSET score=expireAtMillis
    private static final String KEY_WAITING_PREFIX = "queue:waiting:";   // ZSET score=issuedAtMillis
    private static final String KEY_TOKEN_PREFIX = "queue:token:";       // HASH
    private static final String KEY_TOKEN_ID_PREFIX = "queue:token-id:"; // STRING token-id:{concertId}:{userUuid}

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final DefaultRedisScript<String> issueScript;
    private final DefaultRedisScript<Long> promoteScript;

    public RedisQueueManager(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.issueScript = new DefaultRedisScript<>(ISSUE_LUA, String.class);
        this.promoteScript = new DefaultRedisScript<>(PROMOTE_LUA, Long.class);
    }

    @Override
    public QueueEntry addToQueue(String userUuid, Long concertId) {
        validateUserUuid(userUuid);
        if (concertId == null) throw new IllegalArgumentException("concertId must not be null");

        String token = UUID.randomUUID().toString();
        long nowMillis = clock.millis();

        String activeKey = activeKey(concertId);
        String waitingKey = waitingKey(concertId);
        String tokenKey = tokenKey(token);
        String tokenIdKey = tokenIdKey(concertId, userUuid);

        try {
            // result format: "token|STATUS|rank"
            String result = redisTemplate.execute(
                    issueScript,
                    List.of(activeKey, waitingKey, tokenKey, tokenIdKey, KEY_CONCERTS),
                    token,
                    userUuid,
                    String.valueOf(concertId),
                    String.valueOf(nowMillis),
                    String.valueOf(ACTIVE_LIMIT),
                    String.valueOf(ACTIVE_TTL.toMillis()),
                    String.valueOf(WAITING_TTL.toMillis())
            );

            if (result == null || result.isBlank()) {
                throw new IllegalStateException("issueScript returned empty");
            }

            String[] parts = result.split("\\|");
            String issuedToken = parts[0];
            QueueStatus status = QueueStatus.valueOf(parts[1]);
            long rank = Long.parseLong(parts[2]);

            return new QueueEntry(issuedToken, userUuid, concertId, rank, status);

        } catch (DataAccessException e) {
            throw new IllegalStateException("Redis error while issuing token", e);
        }
    }

    @Override
    public Optional<QueueEntry> findByToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        Map<Object, Object> map = redisTemplate.opsForHash().entries(tokenKey(token));
        if (map == null || map.isEmpty()) return Optional.empty();

        String userUuid = (String) map.get("userUuid");
        String concertIdStr = (String) map.get("concertId");
        String statusStr = (String) map.get("status");

        if (userUuid == null || concertIdStr == null || statusStr == null) return Optional.empty();

        Long concertId = Long.valueOf(concertIdStr);
        QueueStatus status = QueueStatus.valueOf(statusStr);

        // hash만 남아있는 “유령 상태” 방지: ZSET 정합성 체크
        if (status == QueueStatus.ACTIVE) {
            Double expireAt = redisTemplate.opsForZSet().score(activeKey(concertId), token);
            if (expireAt == null || expireAt <= clock.millis()) {
                return Optional.of(new QueueEntry(token, userUuid, concertId, -1, QueueStatus.EXPIRED));
            }
            return Optional.of(new QueueEntry(token, userUuid, concertId, 0, QueueStatus.ACTIVE));
        }

        if (status == QueueStatus.ISSUED) {
            Long r = redisTemplate.opsForZSet().rank(waitingKey(concertId), token);
            if (r == null) {
                return Optional.of(new QueueEntry(token, userUuid, concertId, -1, QueueStatus.EXPIRED));
            }
            return Optional.of(new QueueEntry(token, userUuid, concertId, r, QueueStatus.ISSUED));
        }

        return Optional.of(new QueueEntry(token, userUuid, concertId, -1, QueueStatus.EXPIRED));
    }

    @Override
    public Optional<Long> rankOf(String token) {
        return findByToken(token).map(QueueEntry::rank);
    }

    @Override
    public QueueStatus statusOf(String token, String userUuid) {
        if (token == null || token.isBlank()) return QueueStatus.EXPIRED;
        validateUserUuid(userUuid);

        Map<Object, Object> map = redisTemplate.opsForHash().entries(tokenKey(token));
        if (map == null || map.isEmpty()) return QueueStatus.EXPIRED;

        String storedUserUuid = (String) map.get("userUuid");
        String statusStr = (String) map.get("status");
        String concertIdStr = (String) map.get("concertId");

        if (!userUuid.equals(storedUserUuid)) return QueueStatus.EXPIRED;
        if (statusStr == null || concertIdStr == null) return QueueStatus.EXPIRED;

        QueueStatus status = QueueStatus.valueOf(statusStr);
        Long concertId = Long.valueOf(concertIdStr);

        if (status == QueueStatus.ACTIVE) {
            Double expireAt = redisTemplate.opsForZSet().score(activeKey(concertId), token);
            if (expireAt == null || expireAt <= clock.millis()) return QueueStatus.EXPIRED;
            return QueueStatus.ACTIVE;
        }

        if (status == QueueStatus.ISSUED) {
            Long r = redisTemplate.opsForZSet().rank(waitingKey(concertId), token);
            if (r == null) return QueueStatus.EXPIRED;
            return QueueStatus.ISSUED;
        }

        return QueueStatus.EXPIRED;
    }

    @Override
    public void removeByUserAndConcert(String userUuid, Long concertId) {
        validateUserUuid(userUuid);
        if (concertId == null) throw new IllegalArgumentException("concertId must not be null");

        String tokenIdKey = tokenIdKey(concertId, userUuid);
        String token = redisTemplate.opsForValue().get(tokenIdKey);
        if (token == null) return;

        redisTemplate.delete(tokenIdKey);
        redisTemplate.delete(tokenKey(token));
        redisTemplate.opsForZSet().remove(activeKey(concertId), token);
        redisTemplate.opsForZSet().remove(waitingKey(concertId), token);
    }

    @Override
    public long promote(Long concertId, int maxToPromote) {
        if (concertId == null) throw new IllegalArgumentException("concertId must not be null");
        if (maxToPromote <= 0) return 0;

        long nowMillis = clock.millis();

        String activeKey = activeKey(concertId);
        String waitingKey = waitingKey(concertId);

        try {
            Long promoted = redisTemplate.execute(
                    promoteScript,
                    List.of(activeKey, waitingKey),
                    String.valueOf(nowMillis),
                    String.valueOf(ACTIVE_LIMIT),
                    String.valueOf(ACTIVE_TTL.toMillis()),
                    String.valueOf(WAITING_TTL.toMillis()),
                    String.valueOf(maxToPromote)
            );
            return promoted == null ? 0 : promoted;
        } catch (DataAccessException e) {
            log.warn("promote failed concertId={}", concertId, e);
            return 0;
        }
    }

    private static void validateUserUuid(String userUuid) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new IllegalArgumentException("userUuid must not be blank");
        }
    }

    private static String activeKey(Long concertId) { return KEY_ACTIVE_PREFIX + concertId; }
    private static String waitingKey(Long concertId) { return KEY_WAITING_PREFIX + concertId; }
    private static String tokenKey(String token) { return KEY_TOKEN_PREFIX + token; }
    private static String tokenIdKey(Long concertId, String userUuid) {
        return KEY_TOKEN_ID_PREFIX + concertId + ":" + userUuid;
    }

    /**
     * ISSUE 보강 포인트:
     * - active 만료 정리 + waiting 만료 정리(issuedAt <= now - waitTtl)
     * - 기존 (concert,user) 멱등성 매핑이 있으면 기존 토큰 반환
     *
     * KEYS:
     * 1 activeKey(ZSET), 2 waitingKey(ZSET), 3 tokenKey(HASH), 4 tokenIdKey(STRING), 5 concertsKey(SET)
     * ARGV:
     * 1 token, 2 userUuid, 3 concertId, 4 nowMillis, 5 activeLimit, 6 activeTtlMs, 7 waitTtlMs
     */
    private static final String ISSUE_LUA = """
            local activeKey   = KEYS[1]
            local waitingKey  = KEYS[2]
            local tokenKey    = KEYS[3]
            local tokenIdKey  = KEYS[4]
            local concertsKey = KEYS[5]

            local token       = ARGV[1]
            local userUuid    = ARGV[2]
            local concertId   = ARGV[3]
            local nowMillis   = tonumber(ARGV[4])
            local activeLimit = tonumber(ARGV[5])
            local activeTtlMs = tonumber(ARGV[6])
            local waitTtlMs   = tonumber(ARGV[7])

            redis.call('SADD', concertsKey, concertId)

            -- 만료 정리
            redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', nowMillis)
            redis.call('ZREMRANGEBYSCORE', waitingKey, '-inf', nowMillis - waitTtlMs)

            -- (concert,user) 멱등성: 기존 토큰 재사용
            local existing = redis.call('GET', tokenIdKey)
            if existing and existing ~= false then
              local st = redis.call('HGET', 'queue:token:' .. existing, 'status')
              if not st then
                redis.call('DEL', tokenIdKey)
              else
                local cid = redis.call('HGET', 'queue:token:' .. existing, 'concertId')
                if not cid then cid = concertId end

                if st == 'ACTIVE' then
                  local exScore = redis.call('ZSCORE', 'queue:active:' .. cid, existing)
                  if exScore and tonumber(exScore) > nowMillis then
                    return existing .. '|ACTIVE|0'
                  end
                  return existing .. '|EXPIRED|-1'
                end

                if st == 'ISSUED' then
                  local r = redis.call('ZRANK', 'queue:waiting:' .. cid, existing)
                  if not r then
                    return existing .. '|EXPIRED|-1'
                  end
                  return existing .. '|ISSUED|' .. r
                end

                return existing .. '|EXPIRED|-1'
              end
            end

            local activeCount = tonumber(redis.call('ZCARD', activeKey))
            local expireAt = nowMillis + activeTtlMs

            redis.call('HSET', tokenKey,
              'userUuid', userUuid,
              'concertId', concertId,
              'issuedAt', tostring(nowMillis)
            )

            if activeCount < activeLimit then
              redis.call('ZADD', activeKey, expireAt, token)
              redis.call('HSET', tokenKey, 'status', 'ACTIVE')
              redis.call('PEXPIRE', tokenKey, activeTtlMs)
              redis.call('PSETEX', tokenIdKey, activeTtlMs, token)
              return token .. '|ACTIVE|0'
            else
              redis.call('ZADD', waitingKey, nowMillis, token)
              redis.call('HSET', tokenKey, 'status', 'ISSUED')
              redis.call('PEXPIRE', tokenKey, waitTtlMs)
              redis.call('PSETEX', tokenIdKey, waitTtlMs, token)
              local r = redis.call('ZRANK', waitingKey, token)
              if not r then r = -1 end
              return token .. '|ISSUED|' .. r
            end
            """;

    /**
     * PROMOTE 보강 포인트:
     * - active 만료 정리
     * - waiting 만료 정리(issuedAt <= now - waitTtl)
     * - waiting -> active 승급 시:
     *   - token hash status/ttl 갱신
     *   - token-id:{concertId}:{userUuid} TTL도 ACTIVE_TTL로 연장 (멱등성 키 유지)
     *
     * KEYS: 1 activeKey, 2 waitingKey
     * ARGV: 1 nowMillis, 2 activeLimit, 3 activeTtlMs, 4 waitTtlMs, 5 maxToPromote
     */
    private static final String PROMOTE_LUA = """
            local activeKey   = KEYS[1]
            local waitingKey  = KEYS[2]

            local nowMillis    = tonumber(ARGV[1])
            local activeLimit  = tonumber(ARGV[2])
            local activeTtlMs  = tonumber(ARGV[3])
            local waitTtlMs    = tonumber(ARGV[4])
            local maxToPromote = tonumber(ARGV[5])

            -- 만료 정리
            redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', nowMillis)
            redis.call('ZREMRANGEBYSCORE', waitingKey, '-inf', nowMillis - waitTtlMs)

            local activeCount = tonumber(redis.call('ZCARD', activeKey))
            local available = activeLimit - activeCount
            if available <= 0 then
              return 0
            end

            local promoteCount = available
            if promoteCount > maxToPromote then
              promoteCount = maxToPromote
            end

            local promoted = 0
            for i=1, promoteCount do
              local popped = redis.call('ZPOPMIN', waitingKey, 1)
              if (not popped) or (#popped == 0) then
                break
              end

              local token = popped[1]
              local expireAt = nowMillis + activeTtlMs

              redis.call('ZADD', activeKey, expireAt, token)

              local tokenKey = 'queue:token:' .. token
              redis.call('HSET', tokenKey, 'status', 'ACTIVE')
              redis.call('PEXPIRE', tokenKey, activeTtlMs)

              -- token-id TTL 연장
              local concertId = redis.call('HGET', tokenKey, 'concertId')
              local userUuid  = redis.call('HGET', tokenKey, 'userUuid')
              if concertId and userUuid then
                local tokenIdKey = 'queue:token-id:' .. concertId .. ':' .. userUuid
                -- 값은 그대로 두고 TTL만 active로 연장
                redis.call('PEXPIRE', tokenIdKey, activeTtlMs)
              end

              promoted = promoted + 1
            end

            return promoted
            """;
}