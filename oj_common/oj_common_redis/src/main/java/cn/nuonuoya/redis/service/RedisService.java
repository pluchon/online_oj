package cn.nuonuoya.redis.service;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Redis 常用操作封装（值统一以 JSON 序列化存储）
@Component
public class RedisService {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    // ======================== key 操作 ========================

    // 获取剩余有效时间
    public Long getExpire(final String key, final TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // 判断键是否存在
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    // 设置有效时间（秒）
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    // 设置有效时间
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    // 删除单个键，键存在并删除成功返回 true
    public boolean deleteObject(final String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // 批量删除键，返回删除数量
    public Long deleteObject(final Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        return redisTemplate.delete(new ArrayList<Object>(keys));
    }

    // ======================== String 操作 ========================

    // 缓存对象（永不过期）
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 缓存对象并设置有效时间
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    // 键不存在时写入并设置有效时间（原子操作），写入成功返回 true
    public <T> boolean setIfAbsent(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit));
    }

    // 批量缓存对象（单次往返写入）
    public <T> void multiSet(final Map<String, T> map) {
        if (CollectionUtils.isEmpty(map)) {
            return;
        }
        redisTemplate.opsForValue().multiSet(map);
    }

    // 获取缓存对象并转换为目标类型，不存在返回 null
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return convert(value, clazz);
    }

    // 批量获取缓存对象（单次往返拉取），缺失项为 null
    public <T> List<T> multiGetCacheObject(final Collection<String> keys, final Class<T> clazz) {
        if (CollectionUtils.isEmpty(keys)) {
            return new ArrayList<>();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(new ArrayList<Object>(keys));
        if (CollectionUtils.isEmpty(values)) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(convert(value, clazz));
        }
        return result;
    }

    // 按指定增量递增
    public Long increment(final String key, final long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // 递增 1
    public Long increment(final String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // ======================== List 操作 ========================

    // 获取列表长度
    public Long getListSize(final String key) {
        return redisTemplate.opsForList().size(key);
    }

    // 获取列表指定范围的元素，列表为空返回 null
    public <T> List<T> getCacheListByRange(final String key, long start, long end, Class<T> clazz) {
        List<Object> range = redisTemplate.opsForList().range(key, start, end);
        if (CollectionUtils.isEmpty(range)) {
            return null;
        }
        return JSON.parseArray(JSON.toJSONString(range), clazz);
    }

    // 获取列表指定下标的元素，不存在返回 null
    public <T> T getListByIndex(final String key, long index, Class<T> clazz) {
        return convert(redisTemplate.opsForList().index(key, index), clazz);
    }

    // 获取元素在列表中首次出现的下标，不存在返回 null
    public Long indexOfList(final String key, Object value) {
        return redisTemplate.opsForList().indexOf(key, value);
    }

    // 尾部批量插入
    public <T> Long rightPushAll(final String key, Collection<T> list) {
        return redisTemplate.opsForList().rightPushAll(key, new ArrayList<Object>(list));
    }

    // 头部插入
    public <T> Long leftPushForList(final String key, T value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    // 删除列表中首个与 value 相等的元素
    public <T> Long removeForList(final String key, T value) {
        return redisTemplate.opsForList().remove(key, 1L, value);
    }

    // 仅保留列表指定范围内的元素
    public void trimList(final String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    // 将缓存值转换为目标类型
    @SuppressWarnings("unchecked")
    private <T> T convert(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        if (value instanceof String str) {
            return JSON.parseObject(str, clazz);
        }
        return JSON.parseObject(JSON.toJSONString(value), clazz);
    }
}
