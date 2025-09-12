package kr.hhplus.be.server.api.layered.repository.user;

import kr.hhplus.be.server.api.layered.entity.user.UserTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserTokenEntity, Long> {
    void deleteByUserId(Long userId);
}
