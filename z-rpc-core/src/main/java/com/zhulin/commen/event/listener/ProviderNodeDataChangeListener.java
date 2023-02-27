package com.zhulin.commen.event.listener;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.event.handler.ZRpcNodeChangeEvent;
import com.zhulin.registry.URL;
import com.zhulin.registry.zookeeper.ProviderNodeInfo;

import java.util.List;

import static com.zhulin.commen.cache.CommonClientCache.CONNECT_MAP;
import static com.zhulin.commen.cache.CommonClientCache.ZROUTER;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description:
 */
public class ProviderNodeDataChangeListener implements ZRpcListener<ZRpcNodeChangeEvent> {
    @Override
    public void callBack(Object t) {
        ProviderNodeInfo providerNodeInfo = ((ProviderNodeInfo) t);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerNodeInfo.getServiceName());
        for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
            //重置分组信息
            String address = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
            if (address.equals(providerNodeInfo.getAddress())) {
                channelFutureWrapper.setGroup(providerNodeInfo.getGroup());
                //修改权重
                channelFutureWrapper.setWeight(providerNodeInfo.getWeight());
                URL url = new URL();
                url.setServiceName(providerNodeInfo.getServiceName());
                //更新权重
                ZROUTER.updateWeight(url);
                break;
            }
        }
    }
}
