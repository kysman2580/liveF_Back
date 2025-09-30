package org.example.livef_dataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {
		// 1. DataSource 자동 구성 제외 (DB 연결 설정이 없음을 알림)
		DataSourceAutoConfiguration.class,
		// 2. JPA(Hibernate) 자동 구성 제외 (JPA를 사용한다면 추가)
		HibernateJpaAutoConfiguration.class
})
@EnableDiscoveryClient
public class LiveFDataServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveFDataServiceApplication.class, args);
	}

}
