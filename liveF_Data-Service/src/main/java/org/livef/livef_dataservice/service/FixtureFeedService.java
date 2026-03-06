package org.livef.livef_dataservice.service;

import org.livef.livef_dataservice.dto.TodayFixtureDetail;
import reactor.core.publisher.Flux;


public interface FixtureFeedService {

    /**
     * 오늘의 경기 목록을 조회합니다. (1순위: Redis 캐시)
     * @return 캐시에서 가져온 경기 목록 DTO
     */
    Flux<TodayFixtureDetail> getThreeDayFixturesByLeague(int leagueId);

}
