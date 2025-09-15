package kr.hhplus.be.server.api.layered.service.auth;

import kr.hhplus.be.server.api.layered.dto.auth.LoginRequest;
import kr.hhplus.be.server.api.layered.dto.auth.UserToken;
import kr.hhplus.be.server.api.layered.dto.auth.SignupRequest;
import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.entity.user.UserTokenEntity;
import kr.hhplus.be.server.api.layered.repository.user.UserRepository;
import kr.hhplus.be.server.api.layered.repository.user.UserTokenRepository;
import kr.hhplus.be.server.api.layered.service.wallet.WalletService;
import kr.hhplus.be.server.common.exception.*;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @Mock : 가짜 객체 -> when().thenReturn() 으로 행동 정의
 * @InjectMocks : 가짜 객체를 주입받는 객체, 실제 테스트 대상
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletService walletService;
    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("회원 가입")
    class Signup {
        @Test
        void 회원가입_성공() {
            // Given
            SignupRequest request = new SignupRequest("test@example.com", "password123", "홍길동");

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

            // When
            authService.signup(request);

            // Then
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        void 이미_존재하는_이메일로_회원가입_실패() {
            // Given
            SignupRequest request = new SignupRequest("existing@example.com", "password123", "홍길동");
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .extracting(ex -> ((EmailAlreadyExistsException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        @Test
        void 유효하지_않은_이메일_형식으로_회원가입_실패() {
            // Given - @ 없는 잘못된 이메일
            SignupRequest request = new SignupRequest("invalid-email", "password123", "홍길동");

            // When & Then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .extracting(ex -> ((InvalidEmailFormatException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        @Test
        void 빈_이메일로_회원가입_실패() {
            // Given
            SignupRequest request = new SignupRequest("", "password123", "홍길동");

            // When & Then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .extracting(ex -> ((InvalidEmailFormatException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {
        private UserEntity createMockUser() {
            return UserEntity.builder()
                    .id(1L)
                    .userUuid(UUID.randomUUID().toString())
                    .email("test@example.com")
                    .password("encodedPassword")
                    .name("테스트사용자")
                    .build();
        }

        @Test
        void 로그인_성공() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            UserEntity user = createMockUser();

            // Mock 실행 여부
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

            when(jwtTokenProvider.createAccessToken(user.getUserUuid(), null)).thenReturn("mockAccessToken");
            when(jwtTokenProvider.createRefreshToken(user.getUserUuid())).thenReturn("mockRefreshToken");
            when(jwtTokenProvider.getAccessTokenExpiresIn()).thenReturn(3600);
            when(jwtTokenProvider.getRefreshTokenExpiresIn()).thenReturn(86400);

            when(userTokenRepository.save(any(UserTokenEntity.class))).thenReturn(any());

            // When
            UserToken response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getAccessTokenExpiresIn()).isGreaterThan(0);
            assertThat(response.getRefreshTokenExpiresIn()).isGreaterThan(0);

            // Refresh Token 저장 검증 추가
            verify(userTokenRepository).save(any(UserTokenEntity.class));
        }


        @Test
        void 로그인_성공시_리프레시토큰_저장() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            UserEntity user = createMockUser();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
            when(jwtTokenProvider.createAccessToken(user.getUserUuid(), null)).thenReturn("mockAccessToken");
            when(jwtTokenProvider.createRefreshToken(user.getUserUuid())).thenReturn("mockRefreshToken");
            when(jwtTokenProvider.getAccessTokenExpiresIn()).thenReturn(3600);
            when(jwtTokenProvider.getRefreshTokenExpiresIn()).thenReturn(86400);

            // When
            authService.login(request);

            // Then
            verify(userTokenRepository).save(argThat(userToken ->
                    userToken.getUser().getId().equals(user.getId()) &&
                            userToken.getRefreshTokenHash() != null &&
                            userToken.getExpiresAt() != null
            ));
        }


        @Test
        void 존재하지_않는_이메일로_로그인_실패() {
            // Given
            LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            // When & Then - 통합 예외 처리
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(LoginFailedException.class)
                    .extracting(ex -> ((LoginFailedException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }

        @Test
        void 잘못된_패스워드로_로그인_실패() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
            UserEntity user = createMockUser();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

            // When & Then - 동일한 예외 처리 (보안상 이유)
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(LoginFailedException.class)
                    .extracting(ex -> ((LoginFailedException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }

    }


}