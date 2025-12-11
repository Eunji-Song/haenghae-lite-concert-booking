package kr.hhplus.be.server;

import kr.hhplus.be.server.common.lock.TestDistributedLockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class ServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
