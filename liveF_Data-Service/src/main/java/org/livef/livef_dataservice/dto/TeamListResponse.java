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
public class TeamListResponse {
    private String get;
    private int results;

    // JSON 디코딩 오류 해결 (Map으로 유연하게 대응)
    private Object errors;

    private Map<String, String> parameters;
    private Paging paging; // (Paging DTO는 별도로 정의되어야 함)

    // ⭐ response 필드를 새로 만든 TeamApiInfo 목록으로 매핑
    @JsonProperty("response")
    private List<TeamApiInfo> response;
}