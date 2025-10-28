package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class ApiFootballResponse {
    private String get; // 호출된 엔드포인트 이름 (예: "fixtures")
    private int results; // 반환된 결과 개수
    private List<String> errors; // 오류 목록 (없으면 빈 리스트)
    private Map<String, String> parameters; // { "date": "2025-10-18" }
    private Paging paging; // { "current": 1, "total": 1 }

    // 실제 경기 데이터 목록을 담는 필드
    @JsonProperty("response")
    private List<TodayFixtureResponse> response;
}
