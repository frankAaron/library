package com.library.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Redis 缓存服务（热点数据缓存核心组件）
 * <p>
 * 设计要点：
 * 1. 全部方法内部捕获异常并降级——Redis 宕机/不可用时自动回源数据库，不影响业务可用性；
 * 2. 提供 getOrLoad 通用模板：缓存未命中自动执行 loader 回源并回写缓存；
 * 3. ZSet 结构维护热门图书榜单，支持整体重建。
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 反序列化忽略未知字段，避免实体字段演进后旧缓存解析失败
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Lazy
    @Autowired
    private JedisPool jedisPool;

    /** 读取缓存原始 JSON（Redis 异常时返回 null 降级） */
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            log.warn("Redis get 降级, key={}, 原因: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /** 写入缓存（JSON 序列化 + 过期时间，单位秒） */
    public void set(String key, Object value, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, ttlSeconds, OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            log.warn("Redis set 降级, key={}, 原因: {}", key, e.getMessage(), e);
        }
    }

    /** 删除缓存（数据变更后主动失效） */
    public void evict(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            log.warn("Redis evict 降级, key={}, 原因: {}", key, e.getMessage(), e);
        }
    }

    /**
     * 缓存读取/回源通用模板（单对象场景）
     * 先查缓存命中则反序列化返回；未命中执行 loader 查库并回写缓存
     */
    public <T> T getOrLoad(String key, int ttlSeconds, Class<T> clazz, Supplier<T> loader) {
        String json = get(key);
        if (json != null) {
            try {
                return OBJECT_MAPPER.readValue(json, clazz);
            } catch (Exception e) {
                log.warn("缓存反序列化失败, 清除脏数据, key={}", key);
                evict(key);
            }
        }
        T value = loader.get();
        if (value != null) {
            set(key, value, ttlSeconds);
        }
        return value;
    }

    /** 缓存读取/回源通用模板（List 等泛型集合场景） */
    public <T> T getOrLoad(String key, int ttlSeconds, TypeReference<T> type, Supplier<T> loader) {
        String json = get(key);
        if (json != null) {
            try {
                return OBJECT_MAPPER.readValue(json, type);
            } catch (Exception e) {
                log.warn("缓存反序列化失败, 清除脏数据, key={}", key);
                evict(key);
            }
        }
        T value = loader.get();
        if (value != null) {
            set(key, value, ttlSeconds);
        }
        return value;
    }

    /** ZSet 倒序区间查询（热门榜单），返回按分数倒序的成员集合 */
    public Set<String> zRevRange(String key, long start, long end) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrange(key, start, end);
        } catch (Exception e) {
            log.warn("Redis zrevrange 降级, key={}, 原因: {}", key, e.getMessage());
            return null;
        }
    }

    /** 整体重建 ZSet（先删除旧榜单，再批量写入并设置过期时间） */
    public void zRebuild(String key, Map<String, Double> scoreMap, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
            if (scoreMap != null && !scoreMap.isEmpty()) {
                jedis.zadd(key, scoreMap);
                jedis.expire(key, ttlSeconds);
            }
        } catch (Exception e) {
            log.warn("Redis zadd 降级, key={}, 原因: {}", key, e.getMessage());
        }
    }
}