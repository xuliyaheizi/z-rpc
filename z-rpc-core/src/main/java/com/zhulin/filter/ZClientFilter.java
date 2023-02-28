package com.zhulin.filter;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;

import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 客户端过滤器接口
 */
public interface ZClientFilter extends ZFilter {
    /**
     * 过滤器执行核心逻辑
     *
     * @param src
     * @param rpcInfoContent
     */
    void doFilter(List<ChannelFutureWrapper> src, RpcInfoContent rpcInfoContent);
}
