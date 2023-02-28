package com.zhulin.filter;

import com.zhulin.commen.protocol.RpcInfoContent;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务端过滤器接口
 */
public interface ZServerFilter extends ZFilter{

    /**
     *执行核心过滤逻辑
     * @param rpcInfoContent
     */
    void doFilter(RpcInfoContent rpcInfoContent);
}
