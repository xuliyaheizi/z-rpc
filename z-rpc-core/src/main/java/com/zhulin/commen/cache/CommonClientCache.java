package com.zhulin.commen.cache;

import com.zhulin.commen.channel.ChannelFuturePollingRef;
import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.config.ClientConfig;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.registry.URL;
import com.zhulin.router.ZRouter;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 客户端缓存类
 */
public class CommonClientCache {
    /**
     * 发送传输内容的消息队列
     */
    public static BlockingQueue<RpcInfoContent> SEND_QUEUE = new ArrayBlockingQueue<RpcInfoContent>(5000);
    /**
     * 响应消息集合，线程安全
     */
    public static Map<String, Object> RESP_MAP = new ConcurrentHashMap<>();
    /**
     * provider名称 --> 该服务有哪些集群URL
     */
    public static List<URL> SUBSCRIBER_SERVICE_LIST = new ArrayList<>();

    public static ClientConfig CLIENT_CONFIG;
    /**
     * 存储服务生产者的节点信息，线程安全
     * <providerServiceName,<provdierIp,providerNodeInfo>>
     */
    public static Map<String, Map<String, String>> URL_MAP = new ConcurrentHashMap<>();
    /**
     * 存储服务生产者的Ip地址
     */
    public static Set<String> SERVICE_ADDRESS = new HashSet<>();
    /**
     * 存储已连接的服务生产者通道，线程安全
     */
    public static Map<String, List<ChannelFutureWrapper>> CONNECT_MAP = new ConcurrentHashMap<>();
    /**
     * 路由之后的服务
     */
    public static Map<String, ChannelFutureWrapper[]> SERVICE_ROUTER_MAP = new ConcurrentHashMap<>();
    /**
     * 全局路由
     */
    public static ZRouter ZROUTER;
    public static ChannelFuturePollingRef CHANNEL_FUTURE_POLLING = new ChannelFuturePollingRef();
}
