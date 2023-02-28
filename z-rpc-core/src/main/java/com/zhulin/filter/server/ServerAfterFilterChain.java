package com.zhulin.filter.server;

import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZServerFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务端后置过滤器
 */
public class ServerAfterFilterChain {
    private static List<ZServerFilter> zServerAfterFilters = new ArrayList<>();

    public void addZServerFilter(ZServerFilter zServerFilter) {
        zServerAfterFilters.add(zServerFilter);
    }

    public void doFilter(RpcInfoContent rpcInfoContent) {
        for (ZServerFilter zServerAfterFilter : zServerAfterFilters) {
            zServerAfterFilter.doFilter(rpcInfoContent);
        }
    }
}
