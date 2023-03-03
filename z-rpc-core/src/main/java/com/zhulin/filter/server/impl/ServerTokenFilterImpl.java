package com.zhulin.filter.server.impl;

import com.zhulin.commen.annotations.SPI;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.filter.ZServerFilter;
import com.zhulin.server.wrapper.RpcServiceWrapper;

import static com.zhulin.commen.cache.CommonServerCache.PROVIDER_SERVICE_WRAPPER_MAP;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务端权限校验过滤器
 */
@SPI("before")
public class ServerTokenFilterImpl implements ZServerFilter {
    @Override
    public void doFilter(RpcInfoContent rpcInfoContent) {
        String clientToken = String.valueOf(rpcInfoContent.getAttachments().get("serverToken"));
        RpcServiceWrapper rpcServiceWrapper = PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInfoContent.getTargetServiceName());
        String serviceToken = rpcServiceWrapper.getServiceToken();
        if (CommonUtil.isEmpty(serviceToken)) {
            return;
        }
        if (!CommonUtil.isEmpty(clientToken) && clientToken.equals(serviceToken)) {
            return;
        }
        throw new RuntimeException("clientToken is " + clientToken + " , verify is false");
    }
}
