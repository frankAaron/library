package com.library.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Value("${redis.host:127.0.0.1}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    @Value("${redis.timeout:3000}")
    private int timeout;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.maxTotal:50}")
    private int maxTotal;

    @Value("${redis.maxIdle:10}")
    private int maxIdle;

    @Value("${redis.minIdle:5}")
    private int minIdle;

    @Value("${redis.maxWaitMillis:3000}")
    private int maxWaitMillis;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(maxTotal);
        cfg.setMaxIdle(maxIdle);
        cfg.setMinIdle(minIdle);
        cfg.setMaxWaitMillis(Math.min(maxWaitMillis, timeout));
        cfg.setTestOnBorrow(false);
        cfg.setTestOnReturn(false);
        cfg.setTestWhileIdle(true);
        return cfg;
    }

    @Bean
    @Lazy
    public JedisPool jedisPool() {
        JedisPoolConfig cfg = jedisPoolConfig();
        if (password != null && !password.isEmpty()) {
            return new JedisPool(cfg, host, port, timeout, password);
        }
        return new JedisPool(cfg, host, port, timeout);
    }
}