package com.zhulin.filter.client;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZClientFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 客户端过滤器链路设计
 */
public class ClientFilterChain {
    private static List<ZClientFilter> zClientFilters = new ArrayList<>();

    public void addZClientFilter(ZClientFilter zClientFilter) {
        zClientFilters.add(zClientFilter);
    }

    public void doFilter(List<ChannelFutureWrapper> src, RpcInfoContent rpcInfoContent) {
        for (ZClientFilter zClientFilter : zClientFilters) {
            zClientFilter.doFilter(src, rpcInfoContent);
        }
    }

}
