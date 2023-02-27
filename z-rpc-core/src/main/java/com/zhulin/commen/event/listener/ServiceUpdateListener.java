package com.zhulin.commen.event.listener;

import com.zhulin.client.handler.ConnectionHandler;
import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.event.data.URLChangeWrapper;
import com.zhulin.commen.event.handler.ZRpcUpdateEvent;
import com.zhulin.registry.URL;
import com.zhulin.registry.zookeeper.ProviderNodeInfo;
import com.zhulin.router.Selector;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.zhulin.commen.cache.CommonClientCache.CONNECT_MAP;
import static com.zhulin.commen.cache.CommonClientCache.ZROUTER;

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
        //根据服务名获取服务
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(urlChangeWrapper.getServiceName());
        if (channelFutureWrappers.isEmpty()) {
            log.error("[ServiceUpdateListener] channelFutureWrappers is empty");
            return;
        } else {
            List<String> matchProviderUrl = urlChangeWrapper.getProviderUrl();
            Set<String> finalUrl = new HashSet<>();
            List<ChannelFutureWrapper> finalChannelFutureWrappers = new ArrayList<>();
            for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
                String oldServerAddress = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
                //如果老的url没有了，说明已经被移除
                if (!matchProviderUrl.contains(oldServerAddress)) {
                    continue;
                } else {
                    finalChannelFutureWrappers.add(channelFutureWrapper);
                    finalUrl.add(oldServerAddress);
                }
            }
            //此时老的url已经被移除了，开始检查是否有新的url
            List<ChannelFutureWrapper> newChannelFutureWrappers = new ArrayList<>();
            for (String newProviderUrl : matchProviderUrl) {
                if (!finalUrl.contains(newProviderUrl)) {
                    //不存在，则需要添加新的url
                    ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper();
                    String host = newProviderUrl.split(":")[0];
                    Integer port = Integer.valueOf(newProviderUrl.split(":")[1]);
                    channelFutureWrapper.setHost(host);
                    channelFutureWrapper.setPort(port);
                    String urlStr = urlChangeWrapper.getNodeDataUrl().get(newProviderUrl);
                    ProviderNodeInfo providerNodeInfo = URL.buildProviderNodeFromUrlStr(urlStr);
                    channelFutureWrapper.setWeight(providerNodeInfo.getWeight());
                    channelFutureWrapper.setGroup(providerNodeInfo.getGroup());
                    ChannelFuture channelFuture = null;
                    try {
                        channelFuture = new ConnectionHandler().createChannelFuture(host, port);
                        log.debug("channel reconnect,server is {}:{}", host, port);
                        channelFutureWrapper.setChannelFuture(channelFuture);
                        newChannelFutureWrappers.add(channelFutureWrapper);
                        finalUrl.add(newProviderUrl);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            finalChannelFutureWrappers.addAll(newChannelFutureWrappers);
            //最终更新服务
            CONNECT_MAP.put(urlChangeWrapper.getServiceName(), finalChannelFutureWrappers);
            Selector selector = new Selector();
            selector.setProviderServiceName(urlChangeWrapper.getServiceName());
            ZROUTER.refreshRouterArr(selector);
        }
    }
}
