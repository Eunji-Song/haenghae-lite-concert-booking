package kr.hhplus.be.server.product.integration;

import kr.hhplus.be.server.common.integration.BaseIntegrationTest;
import kr.hhplus.be.server.product.api.dto.OpenDateResponse;
import kr.hhplus.be.server.product.application.service.ConcertService;
import kr.hhplus.be.server.product.infrastructure.jpa.adapter.ConcertDateRepositoryAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

@ActiveProfiles("test")
class ConcertServiceCacheIntegrationTest extends BaseIntegrationTest {

    @Autowired
    ConcertService concertService;

    @MockitoSpyBean
    ConcertDateRepositoryAdapter dateAdapter;

    @Autowired
    CacheManager cacheManager;

    @Test
    @DisplayName("getOpenDates: 캐시가 적용되어 같은 요청은 DB를 한 번만 호출한다")
    void getOpenDates_cacheWorks() {
        Long concertId = 777L;

        // 캐시 비우고 시작 (혹시 이전 테스트 영향 제거)
        cacheManager.getCache("concert:openDates").clear();

        // when: 첫 번째 호출 -> 실제 DB(Repository) 호출 + 캐시에 저장
        List<OpenDateResponse> res1 = concertService.getOpenDates(concertId);

        // when: 두 번째 호출 -> 캐시에서 조회 (Repository 호출 X)
        List<OpenDateResponse> res2 = concertService.getOpenDates(concertId);

        // then
        assertThat(res1).isEqualTo(res2);

        // SpyBean 으로 Repository 호출 횟수 검증
        Mockito.verify(dateAdapter, times(1)).getOpenDates(concertId);

        // 캐시에 값이 들어갔는지도 확인
        assertThat(cacheManager.getCache("concert:openDates").get(concertId)).isNotNull();
    }

}