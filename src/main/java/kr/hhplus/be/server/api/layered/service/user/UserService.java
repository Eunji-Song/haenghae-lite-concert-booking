package kr.hhplus.be.server.api.layered.service.user;

import kr.hhplus.be.server.api.layered.entity.user.UserEntity;
import kr.hhplus.be.server.api.layered.repository.user.UserRepository;
import kr.hhplus.be.server.common.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserEntity getUser(String userUuid) {
        return userRepository.findByUserUuid(userUuid)
                .orElseThrow(UserNotFoundException::new);
    }

    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
