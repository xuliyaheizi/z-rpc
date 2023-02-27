package com.zhulin.commen.cache;

import com.zhulin.commen.config.ServerConfig;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.URL;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    public static final Map<String, Object> PROVIDER_MAP = new ConcurrentHashMap<>();

    /**
     * 服务注册集合,Set集合无序、不可重复
     */
    public static final Set<URL> PROVIDER_URL_SET = new HashSet<>();
    public static ServerConfig SERVER_CONFIG;
    /**
     * 注册中心
     */
    public static AbstractRegistry REGISTRY_SERVICE;
}
