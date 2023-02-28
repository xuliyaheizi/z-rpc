package com.zhulin.commen.cache;

import com.zhulin.commen.config.ServerConfig;
import com.zhulin.filter.server.ServerAfterFilterChain;
import com.zhulin.filter.server.ServerBeforeFilterChain;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.URL;
import com.zhulin.serializer.SerializeFactory;
import com.zhulin.server.dispatcher.ServerChannelDispatcher;
import com.zhulin.server.wrapper.RpcServiceWrapper;

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
    /**
     * 服务端的序列化方式
     */
    public static SerializeFactory SERVER_SERIALIZE_FACTORY;

    public static Map<String, RpcServiceWrapper> PROVIDER_SERVICE_WRAPPER_MAP = new ConcurrentHashMap<>();
    /**
     * 服务端前置与后置过滤器
     */
    public static ServerBeforeFilterChain SERVER_BEFORE_FILTER_CHAIN;
    public static ServerAfterFilterChain SERVER_AFTER_FILTER_CHAIN;
    /**
     * 服务端多线程处理客户端请求数据
     */
    public static ServerChannelDispatcher SERVER_CHANNEL_DISPATCHER = new ServerChannelDispatcher();
}
