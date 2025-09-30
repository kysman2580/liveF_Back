package org.example.livef_eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class LiveFEurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveFEurekaServerApplication.class, args);
	}

}
