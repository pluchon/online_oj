package cn.nuonuoya.common.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 基于阿里TTL的线程上下文工具类，支持父子线程与线程池上下文透传
public class ThreadLocalUtil {

    // 线程本地变量容器
    private static final TransmittableThreadLocal<Map<String, Object>> THREAD_LOCAL = new TransmittableThreadLocal<>();

    // 往当前线程上下文中存入键值对
    public static void set(String key, Object value) {
        Map<String, Object> map = getLocalMap();
        map.put(key, value == null ? StrUtil.EMPTY : value);
    }

    // 从当前线程上下文中获取指定类型的对象（通过Convert安全转换）
    public static <T> T get(String key, Class<T> clazz) {
        Map<String, Object> map = getLocalMap();
        Object obj = map.get(key);
        if (obj == null) {
            return null;
        }
        return Convert.convert(clazz, obj);
    }

    // 从当前线程上下文中获取字符串类型的值
    public static String get(String key) {
        return get(key, String.class);
    }

    // 获取当前线程绑定的 Map，若为空则初始化
    public static Map<String, Object> getLocalMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    // 清理当前线程上下文，防止线程池复用污染与内存泄露
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
