package org.livef.livef_apigateway.component;

import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {
    
    public JwtAuthenticationWebFilter(JwtReactiveAuthenticationManager authenticationManager,
                                      JwtServerAuthenticationConverter converter) {
        super(authenticationManager);
        setServerAuthenticationConverter(converter);
    }
}