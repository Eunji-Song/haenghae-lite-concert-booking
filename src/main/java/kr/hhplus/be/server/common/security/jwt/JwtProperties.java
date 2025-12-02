package kr.hhplus.be.server.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Key access = new Key();
    private Key refresh = new Key();
    private Key queue = new Key();

    public Key getAccess() { return access; }
    public void setAccess(Key access) { this.access = access; }

    public Key getRefresh() { return refresh; }
    public void setRefresh(Key refresh) { this.refresh = refresh; }

    public Key getQueue() { return queue; }
    public void setQueue(Key queue) { this.queue = queue; }

    public static class Key {
        private String secret;
        private long expirySeconds;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getExpirySeconds() { return expirySeconds; }
        public void setExpirySeconds(long expirySeconds) { this.expirySeconds = expirySeconds; }
    }
}