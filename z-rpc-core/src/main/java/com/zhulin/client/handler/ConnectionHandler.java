package com.zhulin.client.handler;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.concurrent.TimeoutInvocation;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.registry.URL;
import com.zhulin.registry.zookeeper.ProviderNodeInfo;
import com.zhulin.router.Selector;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.zhulin.commen.cache.CommonClientCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 处理服务的连接，断开，按照服务名筛选
 */
@Slf4j
public class ConnectionHandler {
    /**
     * Netty的核心连接器，专门用于负责和服务构建连接通信
     */
    public static Bootstrap bootstrap;

    public static void setBootstrap(Bootstrap bootstrap) {
        ConnectionHandler.bootstrap = bootstrap;
    }

    /**
     * 构建单个连接通道 元操作，既要处理连接，还要统一将连接进行内存存储管理
     *
     * @param providerServiceName
     * @param providerIp
     */
    public static void connect(String providerServiceName, String providerIp) throws InterruptedException {
        if (bootstrap == null) {
            throw new RuntimeException("bootstrap con not be null");
        }
        //格式错误类型的信息
        if (!providerIp.contains(":")) {
            return;
        }
        //获取地址和端口号
        String[] ipItems = providerIp.split(":");
        String host = ipItems[0];
        Integer port = Integer.parseInt(ipItems[1]);
        //与服务端连接
        ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
        //获取注册中心该服务的节点信息
        String providerURLInfo = URL_MAP.get(providerServiceName).get(providerIp);
        ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(providerURLInfo);
        //实例channelFuture通道包装类
        ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper(channelFuture, host, port,
                providerNodeInfo.getWeight(), providerNodeInfo.getGroup());
        //服务连接之后，将服务提供者的ip添加到缓存中
        SERVICE_ADDRESS.add(providerIp);
        //在CONNECT_MAP中获取服务提供者的信息
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtil.isEmptyList(channelFutureWrappers)) {
            channelFutureWrappers = new ArrayList<>();
        }
        channelFutureWrappers.add(channelFutureWrapper);
        //例如com.zhulin.test.UserService会被放入到一个Map集合中，key是服务的名字，value是对应的channel通道的List集合
        CONNECT_MAP.put(providerServiceName, channelFutureWrappers);
        //设置路由
        Selector selector = new Selector();
        selector.setProviderServiceName(providerServiceName);
        ZROUTER.refreshRouterArr(selector);
    }

    /**
     * 构建channelFuture通道
     *
     * @param ip
     * @param port
     * @return
     * @throws InterruptedException
     */
    public static ChannelFuture createChannelFuture(String ip, Integer port) throws InterruptedException {
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        return channelFuture;
    }

    /**
     * 断开连接
     *
     * @param providerServiceName
     * @param providerIp
     */
    public static void disConnect(String providerServiceName, String providerIp) {
        //移除服务ip
        SERVICE_ADDRESS.remove(providerIp);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtil.isNotEmptyList(channelFutureWrappers)) {
            //服务提供者的信息不为空，则遍历缓存中的连接通道，移除指定的通道
            Iterator<ChannelFutureWrapper> iterator = channelFutureWrappers.iterator();
            while (iterator.hasNext()) {
                ChannelFutureWrapper futureWrapper = iterator.next();
                if (providerIp.equals(futureWrapper.getHost() + ":" + futureWrapper.getPort())) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 通过负载均衡策略获取连接通道
     *
     * @param rpcInfoContent
     * @return
     */
    public static ChannelFuture getChannelFuture(RpcInfoContent rpcInfoContent) {
        String providerServiceName = rpcInfoContent.getTargetServiceName();
        ChannelFutureWrapper[] channelFutureWrappers = SERVICE_ROUTER_MAP.get(providerServiceName);
        if (channelFutureWrappers == null || channelFutureWrappers.length == 0) {
            rpcInfoContent.setRetry(0);
            rpcInfoContent.setE(new RuntimeException("no provider exist for " + providerServiceName));
            rpcInfoContent.setResponse(null);
            //直接交给响应线程那边处理（响应线程在代理类内部的invoke函数中，那边会取出对应的uuid的值，然后判断）
            TimeoutInvocation timeoutInvocation = (TimeoutInvocation) RESP_MAP.get(rpcInfoContent.getUuid());
            timeoutInvocation.setRpcInfoContent(rpcInfoContent);
            RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
            //通知代理类中的响应线程
            timeoutInvocation.release();
            log.error("channelFutureWrappers is null");
            return null;
        }
        //执行过滤器逻辑
        List<ChannelFutureWrapper> channelFutureWrapperList = new ArrayList<>(channelFutureWrappers.length);
        for (int i = 0; i < channelFutureWrappers.length; i++) {
            channelFutureWrapperList.add(channelFutureWrappers[i]);
        }
        CLIENT_FILTER_CHAIN.doFilter(channelFutureWrapperList, rpcInfoContent);
        //通过负载均衡算法获取合适的服务提供者
        Selector selector = new Selector();
        selector.setProviderServiceName(providerServiceName);
        selector.setChannelFutureWrappers(channelFutureWrappers);
        return ZROUTER.select(selector).getChannelFuture();
    }
}
