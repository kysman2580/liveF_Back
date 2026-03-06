package org.livef.livef_dataservice.service;

import reactor.core.publisher.Mono;

public interface StandingService {
    Mono<String> getStandingsByLeague(Integer leagueId);
}
