package kr.hhplus.be.server.identity.application.service;

import kr.hhplus.be.server.common.exception.user.UserNotFoundException;
import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User getUser(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(UserNotFoundException::new);
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}