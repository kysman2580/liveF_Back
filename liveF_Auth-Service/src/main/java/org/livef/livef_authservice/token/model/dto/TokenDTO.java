package org.livef.livef_authservice.token.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenDTO {
    private String refreshToken;
    private String accessToken;
}
