package kr.hhplus.be.server.common.security.jwt;

import jakarta.servlet.http.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         org.springframework.security.core.AuthenticationException e) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing token\"}");
    }
}