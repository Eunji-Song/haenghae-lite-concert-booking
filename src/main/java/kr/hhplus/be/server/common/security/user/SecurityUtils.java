package kr.hhplus.be.server.common.security.user;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static String getCurrentUserUuidOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated: no user in SecurityContext");
        }
        return auth.getName();
    }
    public static Optional<String> getCurrentUserUuid() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(auth != null ? auth.getName() : null);
    }
}