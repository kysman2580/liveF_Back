package org.example.livef_chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.RequiredArgsConstructor;
import org.example.livef_chatservice.handler.RedisSubscriber;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 (Pub/Sub + Redisson)
 */
@RequiredArgsConstructor
@Configuration
public class RedisConfig {

    private final RedisProperties redisProperties;

    // 1. ObjectMapper 빈 정의 (기존 코드)
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // 2. RedisConnectionFactory 정의 (SSL/TLS 적용 포함)
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisProperties.getHost());
        standaloneConfig.setPort(redisProperties.getPort());
        standaloneConfig.setPassword(redisProperties.getPassword());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder();

        // 🚨 SSL/TLS 설정 적용 (ElastiCache 전송 암호화 대응)
        if (redisProperties.getSsl().isEnabled()) {
            clientConfigBuilder.useSsl();
        }

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().build())
                .build();
        clientConfigBuilder.clientOptions(clientOptions);

        return new LettuceConnectionFactory(standaloneConfig, clientConfigBuilder.build());
    }

    // 3. 🚨 RedissonClient 빈 추가 (분산 락/분산 객체 사용 목적)
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 3-1. URL 생성 (redis:// 또는 rediss://)
        String url = String.format(createUrl(), redisProperties.getHost(), redisProperties.getPort());

        config.useSingleServer()
                .setAddress(url)
                .setPassword(redisProperties.getPassword()) // 비밀번호 설정 추가
                // 3-2. SSL 엔드포인트 식별 활성화
                .setSslEnableEndpointIdentification(redisProperties.getSsl().isEnabled());

        return Redisson.create(config);
    }

    // 3-3. 🚨 URL 접두사 생성 메서드 추가
    private String createUrl() {
        if (redisProperties.getSsl().isEnabled()) {
            return "rediss://%s:%d"; // SSL 활성화 시 rediss 사용
        }
        return "redis://%s:%d"; // 일반 통신 시 redis 사용
    }

    // 4. RedisTemplate 정의 (기존 코드)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // 5. RedisMessageListenerContainer 정의 (기존 코드)
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisSubscriber redisSubscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        MessageListenerAdapter adapter = new MessageListenerAdapter(redisSubscriber, "onMessage");
        container.addMessageListener(adapter, new PatternTopic("chat:league:*"));

        return container;
    }
}