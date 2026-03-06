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

    // JSON 디코딩 오류 해결 (Map 또는 List로 올 수 있음)
    private Object errors;

    public boolean hasErrors() {
        if (errors == null)
            return false;
        if (errors instanceof java.util.Collection)
            return !((java.util.Collection<?>) errors).isEmpty();
        if (errors instanceof java.util.Map)
            return !((java.util.Map<?, ?>) errors).isEmpty();
        return true;
    }

    private Map<String, String> parameters;
    private Paging paging; // (Paging DTO는 별도로 정의되어야 함)

    // ⭐ response 필드를 새로 만든 TeamApiInfo 목록으로 매핑
    @JsonProperty("response")
    private List<TeamApiInfo> response;
}