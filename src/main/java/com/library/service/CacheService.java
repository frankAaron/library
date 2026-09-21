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
import redis.clients.jedis.params.SetParams;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

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
            jedis.setex(key, (long) ttlSeconds, OBJECT_MAPPER.writeValueAsString(value));
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

    /** 递增整数计数器，返回递增后的值；key 不存在时会自动创建并返回 1 */
    public long incr(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(key);
        } catch (Exception e) {
            log.warn("Redis incr 降级, key={}, 原因: {}", key, e.getMessage());
            return 0;
        }
    }

    /** 设置过期时间（秒） */
    public void expire(String key, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, (long) ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis expire 降级, key={}, 原因: {}", key, e.getMessage());
        }
    }

    /** 带过期时间的 setnx（同 tryLock 语义，但返回 boolean 更通用） */
    public boolean setnx(String key, String value, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, value, SetParams.setParams().nx().ex((long) ttlSeconds));
            return "OK".equals(result);
        } catch (Exception e) {
            log.warn("Redis setnx 降级, key={}, 原因: {}", key, e.getMessage());
            return true;
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
                jedis.expire(key, (long) ttlSeconds);
            }
        } catch (Exception e) {
            log.warn("Redis zadd 降级, key={}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 分布式锁：尝试获取锁（SETNX + 过期时间）
     *
     * @param key          锁键
     * @param token        持锁者唯一标识（通常用 UUID），释放锁时校验防止误删
     * @param expireSeconds 锁过期时间（秒），防止宕机后死锁
     * @return true 表示获取成功
     */
    public boolean tryLock(String key, String token, long expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, token, SetParams.setParams().nx().ex(expireSeconds));
            return "OK".equals(result);
        } catch (Exception e) {
            log.warn("Redis tryLock 降级, key={}, 原因: {}", key, e.getMessage());
            return true;
        }
    }

    /**
     * 分布式锁：释放锁（Lua 脚本保证原子性，仅持有者可释放）
     */
    public void releaseLock(String key, String token) {
        try (Jedis jedis = jedisPool.getResource()) {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            jedis.eval(script, 1, key, token);
        } catch (Exception e) {
            log.warn("Redis releaseLock 降级, key={}, 原因: {}", key, e.getMessage());
        }
    }

    public long evictPattern(String pattern) {
        long deleted = 0;
        try (Jedis jedis = jedisPool.getResource()) {
            String cursor = "0";
            ScanParams params = new ScanParams().match(pattern).count(100);
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                for (String key : result.getResult()) {
                    deleted += jedis.del(key);
                }
                cursor = result.getCursor();
            } while (!"0".equals(cursor));
        } catch (Exception e) {
            log.warn("Redis evictPattern 降级, pattern={}, 原因: {}", pattern, e.getMessage());
        }
        return deleted;
    }
}