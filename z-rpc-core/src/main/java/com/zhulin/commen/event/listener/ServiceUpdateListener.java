package com.zhulin.commen.event.listener;

import com.zhulin.client.handler.ConnectionHandler;
import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.event.data.URLChangeWrapper;
import com.zhulin.commen.event.handler.ZRpcUpdateEvent;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.registry.URL;
import com.zhulin.registry.zookeeper.ProviderNodeInfo;
import com.zhulin.router.Selector;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.zhulin.commen.cache.CommonClientCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description:
 */
@Slf4j
public class ServiceUpdateListener implements ZRpcListener<ZRpcUpdateEvent> {
    @Override
    public void callBack(Object t) {
        //获取字节点的数据信息
        URLChangeWrapper urlChangeWrapper = (URLChangeWrapper) t;
        if (CommonUtil.isEmptyList(urlChangeWrapper.getProviderUrl())) {
            //如果为空,说明该服务下已经没有服务提供者了
            CONNECT_MAP.remove(urlChangeWrapper.getServiceName());
            SERVICE_ROUTER_MAP.remove(urlChangeWrapper.getServiceName());
            URL_MAP.remove(urlChangeWrapper.getServiceName());
        } else {
            //根据服务名获取已连接的服务
            List<ChannelFutureWrapper> oldChannelFutureWrappers = CONNECT_MAP.get(urlChangeWrapper.getServiceName());
            //获取现有的服务提供者
            List<String> matchProviderUrl = urlChangeWrapper.getProviderUrl();
            //最终的服务提供者IP
            Set<String> finalUrl = new HashSet<>();
            //最终的服务提供者连接通道
            List<ChannelFutureWrapper> finalChannelFutureWrappers = new ArrayList<>();
            //新的服务提供者连接通道
            List<ChannelFutureWrapper> newChannelFutureWrappers = new ArrayList<>();
            if (CommonUtil.isEmptyList(oldChannelFutureWrappers)) {
                //暂未连接到服务提供者
                newChannelFutureWrappers = connectWithNewProvider(urlChangeWrapper, matchProviderUrl);
            } else {
                //遍历旧的服务提供者连接通道
                for (ChannelFutureWrapper channelFutureWrapper : oldChannelFutureWrappers) {
                    String oldServerAddress = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
                    //如果老的url没有了，说明已经被移除
                    if (matchProviderUrl.contains(oldServerAddress)) {
                        finalChannelFutureWrappers.add(channelFutureWrapper);
                        finalUrl.add(oldServerAddress);
                    }
                }
                List<String> newProviderUrl = new ArrayList<>();
                for (String providerUrl : matchProviderUrl) {
                    //判断是否是新的服务提供者
                    if (!finalUrl.contains(providerUrl)) {
                        newProviderUrl.add(providerUrl);
                    }
                }
                //此时老的url已经被移除了，开始检查是否有新的url
                newChannelFutureWrappers = connectWithNewProvider(urlChangeWrapper, newProviderUrl);
            }
            finalChannelFutureWrappers.addAll(newChannelFutureWrappers);
            //最终更新服务
            CONNECT_MAP.put(urlChangeWrapper.getServiceName(), finalChannelFutureWrappers);
            Selector selector = new Selector();
            selector.setProviderServiceName(urlChangeWrapper.getServiceName());
            ZROUTER.refreshRouterArr(selector);
        }
    }

    /**
     * 与新上线的服务提供者建立连接
     *
     * @param urlChangeWrapper
     * @param newProviderUrl
     * @return
     */
    private List<ChannelFutureWrapper> connectWithNewProvider(URLChangeWrapper urlChangeWrapper,
                                                              List<String> newProviderUrl) {
        List<ChannelFutureWrapper> newChannelFutureWrappers = new ArrayList<>();
        for (String providerUrl : newProviderUrl) {
            //不存在，则需要添加新的url
            String host = providerUrl.split(":")[0];
            Integer port = Integer.valueOf(providerUrl.split(":")[1]);
            String urlStr = urlChangeWrapper.getNodeDataUrl().get(providerUrl);
            ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(urlStr);
            ChannelFuture channelFuture = null;
            try {
                //与新的服务提供者建立连接通道
                channelFuture = ConnectionHandler.createChannelFuture(host, port);
                log.debug("channel reconnect,server is {}:{}", host, port);
                ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper(channelFuture, host, port,
                        providerNodeInfo.getWeight(), providerNodeInfo.getGroup());
                newChannelFutureWrappers.add(channelFutureWrapper);
                //监听节点
                URL watchUrl = new URL();
                watchUrl.addParameter("providerPath", providerNodeInfo.getServiceName() + "/provider/" + providerUrl);
                REGISTRY_SERVICE.doAfterSubscribe(watchUrl);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return newChannelFutureWrappers;
    }
}
