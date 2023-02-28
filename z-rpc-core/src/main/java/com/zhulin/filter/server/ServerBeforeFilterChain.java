package com.zhulin.filter.server;

import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.filter.ZServerFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务端前置过滤器
 */
public class ServerBeforeFilterChain {
    private static List<ZServerFilter> zServerBeforeFilters = new ArrayList<>();

    public void addZServerFilter(ZServerFilter zServerFilter) {
        zServerBeforeFilters.add(zServerFilter);
    }

    public void doFilter(RpcInfoContent rpcInfoContent) {
        for (ZServerFilter zServerBeforeFilter : zServerBeforeFilters) {
            zServerBeforeFilter.doFilter(rpcInfoContent);
        }
    }
}
