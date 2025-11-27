package org.livef.livef_dataservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // Config Server의 설정 파일(예: livef-data-service.yml)에서 주입받습니다.
    @Value("${api-football.base-url}")
    private String apiFootballBaseUrl;

    @Value("${api-football.api-key}")
    private String apiFootballApiKey;

    @Bean
    public WebClient apiFootballWebClient() {

        // 1. DataBufferLimitException 해결을 위해 버퍼 크기 설정 (예: 4MB)
        final int maxInMemorySize = (int) DataSize.ofMegabytes(4).toBytes();

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();

        return WebClient.builder()
                .baseUrl(apiFootballBaseUrl)
                // 2. 버퍼 크기 확장 전략 적용
                .exchangeStrategies(strategies)

                // 3. 기존 헤더 설정 유지
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-rapidapi-key", apiFootballApiKey)
                .defaultHeader("x-rapidapi-host", "v3.football.api-sports.io")
                .build();
    }
}
