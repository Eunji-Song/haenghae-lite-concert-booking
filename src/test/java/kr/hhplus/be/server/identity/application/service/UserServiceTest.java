package kr.hhplus.be.server.identity.application.service;

import kr.hhplus.be.server.common.exception.user.UserNotFoundException;
import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }

    @Test @DisplayName("userUuid로 조회 성공")
    void getUser_by_uuid() {
        var user = new User("uuid-1","a@a.com","A","hash");
        when(userRepository.findByUuid("uuid-1")).thenReturn(Optional.of(user));

        var found = userService.getUser("uuid-1");
        assertThat(found.getEmail()).isEqualTo("a@a.com");
    }

    @Test @DisplayName("userUuid로 조회 실패 → 404")
    void getUser_by_uuid_not_found() {
        when(userRepository.findByUuid("none")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUser("none")).isInstanceOf(UserNotFoundException.class);
    }

    @Test @DisplayName("ID로 조회 성공")
    void getUser_by_id() {
        var user = new User(10L,"uuid-1","a@a.com","A","hash");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        var found = userService.getUser(10L);
        assertThat(found.getUserUuid()).isEqualTo("uuid-1");
    }
}