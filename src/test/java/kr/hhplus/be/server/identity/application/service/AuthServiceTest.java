package kr.hhplus.be.server.identity.application.service;

import kr.hhplus.be.server.common.exception.user.EmailAlreadyExistsException;
import kr.hhplus.be.server.common.exception.user.LoginFailedException;
import kr.hhplus.be.server.common.fixture.Users;
import kr.hhplus.be.server.common.security.jwt.JwtTokenProvider;
import kr.hhplus.be.server.identity.api.dto.LoginRequest;
import kr.hhplus.be.server.identity.api.dto.SignupRequest;
import kr.hhplus.be.server.identity.api.dto.UserToken;
import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import kr.hhplus.be.server.identity.domain.repository.UserTokenRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserTokenRepository userTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;

    @InjectMocks AuthService authService;

    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }

    @Test @DisplayName("회원가입 성공")
    void signup_success() {
        var req = new SignupRequest("new@hhplus.com", "Plain!234", "뉴유저");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(req.password())).thenReturn("$2a$encoded");

        var saved = new User(UUID.randomUUID().toString(), req.email(), req.name(), "$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var created = authService.createUser(req);

        assertThat(created.getEmail()).isEqualTo("new@hhplus.com");
        verify(userRepository).save(any(User.class));
    }

    @Test @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_duplicateEmail() {
        var req = new SignupRequest("dup@hhplus.com", "x", "중복");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(Users.domainWith("u-1", req.email())));
        assertThatThrownBy(() -> authService.createUser(req)).isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test @DisplayName("로그인 성공 - 토큰 발급")
    void login_success() {
        var req = new LoginRequest("user@hhplus.com", "Correct!234");
        var user = new User("u-uuid", req.email(), "유저", "$2a$hash");

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(eq(user.getUserUuid()), anyMap())).thenReturn("access.jwt");
        when(jwtTokenProvider.createRefreshToken(user.getUserUuid())).thenReturn("refresh.jwt");
        when(jwtTokenProvider.getAccessTokenExpiresIn()).thenReturn(1800);
        when(jwtTokenProvider.getRefreshTokenExpiresIn()).thenReturn(1209600);

        UserToken token = authService.login(req);

        assertThat(token.accessToken()).isEqualTo("access.jwt");
        verify(userTokenRepository).replace(eq("u-uuid"), eq("refresh.jwt"), any(LocalDateTime.class));
    }

    @Test @DisplayName("로그인 실패 - 이메일 없음")
    void login_email_not_found() {
        var req = new LoginRequest("none@hhplus.com", "x");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(LoginFailedException.class);
    }

    @Test @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrong_password() {
        var req = new LoginRequest("user@hhplus.com", "wrong");
        var user = Users.domainWith("u-1", req.email());
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.getPasswordHash())).thenReturn(false);
        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(LoginFailedException.class);
    }
}