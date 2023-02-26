package com.zhulin.proxy;

import com.zhulin.client.reference.RpcReferenceWrapper;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 代理接口
 */
public interface ProxyFactory {

    <T> T getProxy(RpcReferenceWrapper rpcReferenceWrapper);
}
