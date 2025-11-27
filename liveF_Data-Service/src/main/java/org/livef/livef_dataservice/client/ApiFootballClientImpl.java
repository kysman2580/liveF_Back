package org.livef.livef_dataservice.client;

import lombok.RequiredArgsConstructor;
import org.livef.livef_dataservice.dto.ApiFootballResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class ApiFootballClientImpl implements ApiFootballClient {

    private final WebClient webClient;

    @Override
    public Mono<ApiFootballResponse> fetchFixturesByDate(String date) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(ApiFootballResponse.class)
                .onErrorResume(e -> {
                    System.err.println("Error fetching fixtures: " + e.getMessage());
                    return Mono.empty();
                });
    }
}

