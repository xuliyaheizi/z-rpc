package com.zhulin.router;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.registry.URL;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 路由层接口
 */
public interface ZRouter {

    /**
     * 刷新路由数组
     *
     * @param selector
     */
    void refreshRouterArr(Selector selector);

    /**
     * 获取到请求的连接通道
     *
     * @param selector
     * @return
     */
    ChannelFutureWrapper select(Selector selector);

    /**
     * 更新权重
     * @param url
     */
    void updateWeight(URL url);
}
