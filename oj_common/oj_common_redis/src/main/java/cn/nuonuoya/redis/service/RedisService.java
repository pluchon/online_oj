package cn.nuonuoya.redis.service;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 统一封装操作方法
@Component
public class RedisService {

    @Autowired
    public RedisTemplate redisTemplate;

    //************************ 操作key ***************************

    /**
     * 获取剩余的有效时间，可以指定返回时间的单位
     * @param key Redis键值
     * @param time 返回的时间，可以自己设定
     * @return 得到返回时间
     */
    public Long getExpire(final String key,final TimeUnit time){
        return redisTemplate.getExpire(key,time);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    //************************ 操作String类型 ***************************

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    // 批量设置基本对象（单次RTT写入）
    public <T> void multiSet(final Map<String, T> map) {
        if (CollectionUtils.isEmpty(map)) {
            return;
        }
        redisTemplate.opsForValue().multiSet(map);
    }

    /**
     * 批量删除对象
     *
     * @param keys 待删除键集合
     * @return 成功删除的数量
     */
    public Long deleteObject(final Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        T t = operation.get(key);
        if (t instanceof String) {
            return t;
        }
        return JSON.parseObject(String.valueOf(t), clazz);
    }

    /**
     * 批量获取缓存的基本对象（单次RTT拉取）
     *
     * @param keys  缓存键值集合
     * @param clazz 待转换的目标Class
     * @return 缓存数据集合
     */
    public <T> List<T> multiGetCacheObject(final Collection<String> keys, final Class<T> clazz) {
        if (CollectionUtils.isEmpty(keys)) {
            return new ArrayList<>();
        }
        List list = redisTemplate.opsForValue().multiGet(keys);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item == null) {
                result.add(null);
            } else if (clazz.isInstance(item)) {
                result.add(clazz.cast(item));
            } else {
                result.add(JSON.parseObject(JSON.toJSONString(item), clazz));
            }
        }
        return result;
    }

    /**
     * 按指定增量递增
     *
     * @param key   键
     * @param delta 增量
     * @return 递增后的值
     */
    public Long increment(final String key, final long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递增1
     *
     * @param key 键
     * @return 递增后的值
     */
    public Long increment(final String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    //*************** 操作list结构 ****************

    /**
     * 获取list中存储数据数量
     *
     * @param key
     * @return
     */
    public Long getListSize(final String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 获取list中指定范围数据
     *
     * @param key
     * @param start
     * @param end
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end, Class<T> clazz) {
        List range = redisTemplate.opsForList().range(key, start, end);
        if (CollectionUtils.isEmpty(range)) {
            return null;
        }
        return JSON.parseArray(JSON.toJSONString(range), clazz);
    }

    /**
     * 底层使用list结构存储数据(尾插 批量插入)
     */
    public <T> Long rightPushAll(final String key, Collection<T> list) {
        return redisTemplate.opsForList().rightPushAll(key, list);
    }

    /**
     * 底层使用list结构存储数据(头插)
     */
    public <T> Long leftPushForList(final String key, T value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 底层使用list结构,删除指定数据
     */
    public <T> Long removeForList(final String key, T value) {
        return redisTemplate.opsForList().remove(key, 1L, value);
    }

    //************************ 操作Hash类型 ***************************

    public <T> T getCacheMapValue(final String key, final String hKey, Class<T> clazz) {
        Object cacheMapValue = redisTemplate.opsForHash().get(key, hKey);
        if (cacheMapValue != null) {
            return JSON.parseObject(String.valueOf(cacheMapValue), clazz);
        }
        return null;
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @param clazz 待转换对象类型
     * @param <T>   泛型
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<String> hKeys, Class<T> clazz) {
        List list = redisTemplate.opsForHash().multiGet(key, hKeys);
        List<T> result = new ArrayList<>();
        for (Object item : list) {
            result.add(JSON.parseObject(JSON.toJSONString(item), clazz));
        }
        return result;
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final
    T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <K, T> void setCacheMap(final String key, final Map<K, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    public Long deleteCacheMapValue(final String key, final String hKey) {
        return redisTemplate.opsForHash().delete(key, hKey);
    }
}
