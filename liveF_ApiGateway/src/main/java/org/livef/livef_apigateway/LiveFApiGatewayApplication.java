package org.livef.livef_apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;

@SpringBootApplication(
		exclude = {ReactiveSecurityAutoConfiguration.class}  // ← 이거 추가!
)
public class LiveFApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveFApiGatewayApplication.class, args);
	}

}
