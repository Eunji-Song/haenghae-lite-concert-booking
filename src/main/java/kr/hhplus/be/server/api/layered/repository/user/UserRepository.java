package kr.hhplus.be.server.api.layered.repository.user;

import kr.hhplus.be.server.api.layered.entity.user.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UsersEntity, Long> {
}
