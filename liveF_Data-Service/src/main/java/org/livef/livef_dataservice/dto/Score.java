package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Score {
    @JsonProperty("halftime")
    private ScoreDetail halftime; // 필드명을 JSON과 일치시킴

    @JsonProperty("fulltime")
    private ScoreDetail fulltime; // 필드명을 JSON과 일치시킨 (camelCase 제거)

    @JsonProperty("extratime")
    private ScoreDetail extratime; // 필드명을 JSON과 일치시킴

    @JsonProperty("penalty")
    private ScoreDetail penalty;
}