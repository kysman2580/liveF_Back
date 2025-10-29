package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Fixture {
    private int id;
    private String referee;

    // ISO 8601 형식의 시간 문자열 (WebClient + Jackson이 Instant로 파싱 가능)
    @JsonProperty("date")
    private Instant dateTime;

    private long timestamp;

    private Periods periods; // 추가

    private Venue venue; // 추가

    // 경기 상태 정보 (Status)
    @JsonProperty("status")
    private Status status;
}
