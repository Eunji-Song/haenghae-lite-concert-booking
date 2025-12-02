package kr.hhplus.be.server.common.integration;

import kr.hhplus.be.server.common.lock.TestDistributedLockConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestDistributedLockConfig.class})
@Transactional
public abstract class BaseIntegrationTest {
}