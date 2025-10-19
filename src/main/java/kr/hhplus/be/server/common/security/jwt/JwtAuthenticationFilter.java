package kr.hhplus.be.server.common.security.jwt;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.*;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    public JwtAuthenticationFilter(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                var jws = jwt.parseAccess(token);
                String userUuid = jws.getBody().getSubject();

                // 필요 시 DB에서 권한 조회 or 토큰 클레임에 roles 포함
                var authToken = new UsernamePasswordAuthenticationToken(
                        userUuid, null, List.of() // 권한은 나중에
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (JwtException e) {
                // 유효하지 않은 토큰은 무시(엔트리포인트가 처리)
            }
        }
        chain.doFilter(req, res);
    }
}