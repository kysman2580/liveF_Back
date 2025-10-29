package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class League {

    private int id;
    private String name;
    private String type;
    private String logo;
    @JsonProperty("country")
    private String country;
    private int season;
    private String round;
}
