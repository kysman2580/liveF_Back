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
public class StandingResponse {
    private String get;
    private int results;
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
    private Paging paging;

    @JsonProperty("response")
    private List<LeagueStanding> response;
}
