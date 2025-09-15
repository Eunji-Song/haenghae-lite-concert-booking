package kr.hhplus.be.server.api.layered.service.auth;

import kr.hhplus.be.server.api.layered.dto.auth.LoginRequest;
import kr.hhplus.be.server.api.layered.dto.auth.UserToken;
import kr.hhplus.be.server.api.layered.dto.auth.SignupRequest;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserTokenEntity;
import kr.hhplus.be.server.api.layered.repository.user.UserRepository;
import kr.hhplus.be.server.api.layered.repository.user.UserTokenRepository;
import kr.hhplus.be.server.api.layered.service.wallet.WalletService;
import kr.hhplus.be.server.common.exception.EmailAlreadyExistsException;
import kr.hhplus.be.server.common.exception.InvalidEmailFormatException;
import kr.hhplus.be.server.common.exception.LoginFailedException;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final WalletService walletService;


    public void signup(SignupRequest request) {
        validateEmailFormat(request.getEmail());
        validateEmailNotExists(request.getEmail());

        UserEntity user = createUser(request);
        userRepository.save(user);

        walletService.createWallet(user.getUserUuid());
    }

    @Transactional
    public UserToken login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(LoginFailedException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new LoginFailedException();
        }

        return createUserToken(user);
    }

    // === 회원가입 ===
    private UserEntity createUser(SignupRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        return UserEntity.builder()
                .userUuid(generateUserUuid())
                .email(request.getEmail())
                .name(request.getName())
                .password(encodedPassword)
                .build();
    }

    private String generateUserUuid() {
        return UUID.randomUUID().toString();
    }

    private void validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException();
        }
    }

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
    }

    // === 로그인 ===
    private UserToken createUserToken(UserEntity user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserUuid(), null);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserUuid());


        UserToken userToken = new UserToken(
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresIn(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiresIn()
        );

        saveRefreshToken(user, refreshToken);

        return userToken;
    }

    private void saveRefreshToken(UserEntity user, String refreshToken) {
        // 기존 토큰 삭제
        userTokenRepository.deleteByUserId(user.getId());

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpiresIn()
        );

        UserTokenEntity userToken = UserTokenEntity.builder()
                .user(user)
                .refreshTokenHash(refreshToken)
                .expiresAt(expiresAt)
                .build();

        userTokenRepository.save(userToken);
    }


}