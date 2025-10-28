package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Goals {
    private Integer home; // 홈 팀 득점
    private Integer away; // 어웨이 팀 득점
}
