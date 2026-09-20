package com.library.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 6379;
    private static final int TIMEOUT = 3000;
    private static final String PASSWORD = null;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(50);
        cfg.setMaxIdle(10);
        cfg.setMinIdle(5);
        cfg.setMaxWaitMillis(3000);
        cfg.setTestOnBorrow(true);
        return cfg;
    }

    @Bean
    @Lazy
    public JedisPool jedisPool() {
        JedisPoolConfig cfg = jedisPoolConfig();
        if (PASSWORD != null && !PASSWORD.isEmpty()) {
            return new JedisPool(cfg, HOST, PORT, TIMEOUT, PASSWORD);
        }
        return new JedisPool(cfg, HOST, PORT, TIMEOUT);
    }
}