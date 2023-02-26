package com.zhulin.proxy.jdk;

import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.commen.constants.RpcConstants;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.concurrent.TimeoutInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.zhulin.commen.cache.CommonClientCache.RESP_MAP;
import static com.zhulin.commen.cache.CommonClientCache.SEND_QUEUE;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description:
 */
public class JDKInvocationHandler implements InvocationHandler {
    private RpcReferenceWrapper rpcReferenceWrapper;
    /**
     * 超时时间
     */
    private Integer timeOut = RpcConstants.DEFAULT_TIME;

    public JDKInvocationHandler(RpcReferenceWrapper rpcReferenceWrapper) {
        this.rpcReferenceWrapper = rpcReferenceWrapper;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //通过代理技术构建传输内容
        RpcInfoContent rpcInfoContent = new RpcInfoContent();
        rpcInfoContent.setTargetMethod(method.getName());
        rpcInfoContent.setTargetServiceName(rpcReferenceWrapper.getAimClass().getName());
        rpcInfoContent.setArgs(args);
        //通过uui对每一次的请求做单独区分
        rpcInfoContent.setUuid(UUID.randomUUID().toString());
        return tryFinishedTask(rpcInfoContent);
    }

    private Object tryFinishedTask(RpcInfoContent rpcInfoContent) throws InterruptedException, TimeoutException {
        //将传输内容添加到消息队列中
        SEND_QUEUE.add(rpcInfoContent);
        //将该请求保存在响应集合中
        TimeoutInvocation timeoutInvocation = new TimeoutInvocation(null);
        RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);

        //判断是否超时
        if (timeoutInvocation.tryAcquire(timeOut, TimeUnit.MILLISECONDS)) {
            return ((TimeoutInvocation) RESP_MAP.remove(rpcInfoContent.getUuid())).getRpcInfoContent().getResponse();
        }
        RESP_MAP.remove(rpcInfoContent.getUuid());
        throw new TimeoutException("Wait for response from server on client " + timeOut + "ms,service's name is " +
                rpcInfoContent.getTargetServiceName() + "#" + rpcInfoContent.getTargetMethod());
    }
}
