package kr.hhplus.be.server.identity.infrastructure.jpa.adapter;

import kr.hhplus.be.server.identity.domain.model.User;
import kr.hhplus.be.server.identity.domain.repository.UserRepository;
import kr.hhplus.be.server.identity.infrastructure.jpa.entity.UserEntity;
import kr.hhplus.be.server.identity.infrastructure.jpa.mapper.UserJpaMapper;
import kr.hhplus.be.server.identity.infrastructure.jpa.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByUuid(String userUuid) {
        return userJpaRepository.findByUserUuid(userUuid).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(UserJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = UserJpaMapper.toEntity(user);
        UserEntity saved = userJpaRepository.save(entity);
        return UserJpaMapper.toDomain(saved);
    }
}