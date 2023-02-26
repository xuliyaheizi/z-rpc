package com.zhulin.commen.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 服务端缓存类
 */
public class CommonServerCache {
    /**
     * 服务提供者集合
     */
    public static Map<String, Object> PROVIDER_MAP = new ConcurrentHashMap<>();
}
