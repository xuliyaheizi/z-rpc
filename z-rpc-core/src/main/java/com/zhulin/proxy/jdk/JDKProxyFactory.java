package com.zhulin.proxy.jdk;

import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.proxy.ProxyFactory;

import java.lang.reflect.Proxy;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 通过jdk实现代理技术
 */
public class JDKProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(RpcReferenceWrapper rpcReferenceWrapper) {
        return (T) Proxy.newProxyInstance(rpcReferenceWrapper.getAimClass().getClassLoader(),
                new Class[]{rpcReferenceWrapper.getAimClass()}, new JDKInvocationHandler(rpcReferenceWrapper));
    }
}
