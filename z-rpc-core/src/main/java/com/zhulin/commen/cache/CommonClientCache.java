package com.zhulin.commen.cache;

import com.zhulin.commen.protocol.RpcInfoContent;

import java.util.Map;
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
     * 响应消息集合
     */
    public static Map<String, Object> RESP_MAP = new ConcurrentHashMap<>();
}
