package com.zhulin.client.reference;

import com.zhulin.proxy.ProxyFactory;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 代理类
 */
@Data
@AllArgsConstructor
public class RpcReference {

    private ProxyFactory proxyFactory;

    /**
     * 根据接口类型获取代理对象
     *
     * @param rpcReferenceWrapper
     * @param <T>
     * @return
     */
    public <T> T get(RpcReferenceWrapper<T> rpcReferenceWrapper) {
        return proxyFactory.getProxy(rpcReferenceWrapper);
    }
}
