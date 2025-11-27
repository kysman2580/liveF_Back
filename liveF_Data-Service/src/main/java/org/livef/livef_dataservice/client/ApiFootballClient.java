package org.livef.livef_dataservice.client;

import org.livef.livef_dataservice.dto.ApiFootballResponse;
import reactor.core.publisher.Mono;

public interface ApiFootballClient {

    Mono<ApiFootballResponse> fetchFixturesByDate(String date);

}
