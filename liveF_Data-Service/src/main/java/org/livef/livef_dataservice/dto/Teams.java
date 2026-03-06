package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Teams {
    @JsonProperty("home")
    private Team home;

    @JsonProperty("away")
    private Team away;
}
