package kr.hhplus.be.server.common.security.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 현재 인증된 사용자의 uuid(subject)를 Optional로 반환한다.
     * 인증이 없거나 익명 사용자면 Optional.empty()
     */
    public static Optional<String> getCurrentUserUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return Optional.empty();
        }

        // 프로젝트에 따라 principal이 String(subject)일 수도 있고, UserDetails일 수도 있음.
        // 지금은 subject(uuid)를 String으로 쓰는 케이스를 우선 처리.
        if (principal instanceof String s) {
            // 흔히 anonymousUser 같은 값이 들어올 수 있으니 방어
            if ("anonymousUser".equalsIgnoreCase(s)) {
                return Optional.empty();
            }
            return Optional.of(s);
        }

        // 만약 커스텀 UserDetails를 쓰고 있다면 여기서 꺼내도록 확장
        // 예: if (principal instanceof CustomUserDetails cud) return Optional.of(cud.getUserUuid());

        return Optional.empty();
    }
}