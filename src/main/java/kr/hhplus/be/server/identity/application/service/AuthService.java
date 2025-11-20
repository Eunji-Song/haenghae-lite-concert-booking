package kr.hhplus.be.server.identity.application.service;


import kr.hhplus.be.server.common.exception.user.EmailAlreadyExistsException;
import kr.hhplus.be.server.common.exception.user.LoginFailedException;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import kr.hhplus.be.server.identity.api.dto.LoginRequest;
import kr.hhplus.be.server.identity.api.dto.SignupRequest;
import kr.hhplus.be.server.identity.api.dto.UserToken;
import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import kr.hhplus.be.server.identity.domain.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** 이메일 중복 검증 */
    public void validateEmailNotExists(String email) {
        userRepository.findByEmail(email)
                .ifPresent(u -> { throw new EmailAlreadyExistsException(); });
    }

    /** 회원 생성 (DTO → 도메인) 후 저장 */
    @Transactional
    public User createUser(SignupRequest request) {
        validateEmailNotExists(request.email());

        String passwordHash = passwordEncoder.encode(request.password());
        String uuid = UUID.randomUUID().toString();

        User user = new User(uuid, request.email(), request.name(), passwordHash);
        return userRepository.save(user);
    }

    /** 로그인 (인증 → 토큰 발급) */
    @Transactional
    public UserToken login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(LoginFailedException::new);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new LoginFailedException();
        }

        // 토큰 발급
        String uuid = user.userUuid();

        String accessToken = jwtTokenProvider.createAccessToken(uuid, Map.of("email", user.email()));
        String refreshToken = jwtTokenProvider.createRefreshToken(uuid);

        int accessTtl = (int) jwtTokenProvider.getAccessTokenExpiresIn();
        int refreshTtl = (int) jwtTokenProvider.getRefreshTokenExpiresIn();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTtl);
        userTokenRepository.replace(user.userUuid(), refreshToken, expiresAt);

        return new UserToken(accessToken, accessTtl, refreshToken, refreshTtl);
    }
}