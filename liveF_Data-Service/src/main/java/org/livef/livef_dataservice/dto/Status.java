package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Status {
    // API-Football JSON: "short": "NS"
    @JsonProperty("short")
    private String shortName;

    // API-Football JSON: "long": "Not Started"
    @JsonProperty("long")
    private String longName;

    // API-Football JSON: "elapsed": null 또는 숫자
    @JsonProperty("elapsed")
    private Integer elapsed;
}