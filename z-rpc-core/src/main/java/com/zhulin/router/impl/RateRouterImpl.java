package com.zhulin.router.impl;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.registry.URL;
import com.zhulin.router.Selector;
import com.zhulin.router.ZRouter;

import java.util.List;

import static com.zhulin.commen.cache.CommonClientCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 轮询策略
 */
public class RateRouterImpl implements ZRouter {
    @Override
    public void refreshRouterArr(Selector selector) {
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
        ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        for (int i = 0; i < channelFutureWrappers.size(); i++) {
            arr[i] = channelFutureWrappers.get(i);
        }
        SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(), arr);
    }

    @Override
    public ChannelFutureWrapper select(Selector selector) {
        return CHANNEL_FUTURE_POLLING.getChannelFutureWrapper(selector);
    }

    @Override
    public void updateWeight(URL url) {

    }
}
