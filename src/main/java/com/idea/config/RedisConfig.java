package com.idea.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * ElastiCache (Redis-compatible) connection config.
 *
 * ElastiCache Serverless and cluster-mode-disabled replication groups
 * expose a single TLS endpoint — this config connects to that endpoint.
 *
 * TLS is always enabled. AUTH token is optional (set REDIS_AUTH_TOKEN
 * only if your ElastiCache cluster has in-transit encryption + auth enabled).
 *
 * Local dev: spring.data.redis.* in application-dev.yml points to the
 * plain docker-compose Redis container (TLS disabled via profile override).
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String authToken;

    @Value("${spring.data.redis.ssl.enabled:true}")
    private boolean tlsEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        var serverConfig = new RedisStandaloneConfiguration(host, port);

        // AUTH token — ElastiCache uses the Redis AUTH password field
        if (authToken != null && !authToken.isBlank()) {
            serverConfig.setPassword(authToken);
        }

        LettuceClientConfiguration clientConfig;

        if (tlsEnabled) {
            // ElastiCache presents a valid AWS-signed cert — no custom trust store needed
            clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .useSsl()
                .and()
                .build();
        } else {
            // local dev (plain Redis via docker-compose)
            clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .build();
        }

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        var template = new RedisTemplate<String, String>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
