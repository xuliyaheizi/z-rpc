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
    private Integer timeOut = RpcConstants.DEFAULT_TIMEOUT;

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
        rpcInfoContent.setAttachments(rpcReferenceWrapper.getAttachments());
        rpcInfoContent.setRetry(rpcReferenceWrapper.getRetry());
        return tryFinishedTask(rpcInfoContent, rpcReferenceWrapper);
    }

    /**
     * 超时等待
     *
     * @param rpcInfoContent
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     */
    private Object tryFinishedTask(RpcInfoContent rpcInfoContent, RpcReferenceWrapper rpcReferenceWrapper) throws InterruptedException, TimeoutException {
        //将传输内容添加到消息队列中
        SEND_QUEUE.add(rpcInfoContent);
        if (rpcReferenceWrapper.isAsync()) {
            //不需要获取响应数据，只需要向服务端发起一步请求即可
            return null;
        }
        //将该请求保存在响应集合中
        TimeoutInvocation timeoutInvocation = new TimeoutInvocation(null);
        RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);

        //判断是否超时 或者 是否设置了重试机制
        Boolean isTimeOut = timeoutInvocation.tryAcquire(timeOut, TimeUnit.MILLISECONDS);
        //重试次数
        int retryTimes = 0;
        if (isTimeOut || rpcInfoContent.getRetry() > 0) {
            RpcInfoContent respInfo =
                    ((TimeoutInvocation) RESP_MAP.remove(rpcInfoContent.getUuid())).getRpcInfoContent();
            if (respInfo.getRetry() == 0 && rpcInfoContent.getE() == null) {
                //正常结果
                return respInfo.getResponse();
            } else if (rpcInfoContent.getE() != null) {
                if (rpcInfoContent.getRetry() == 0) {
                    return rpcInfoContent.getResponse();
                }
                //在超时情况下才会发起重试机制
                if (isTimeOut) {
                    retryTimes++;
                    //重新发起请求
                    TimeoutInvocation invocation = new TimeoutInvocation(null);
                    RESP_MAP.put(rpcInfoContent.getUuid(), invocation);
                    SEND_QUEUE.add(rpcInfoContent);
                    //回调接收重试之后的响应数据
                    tryFinishedTask(rpcInfoContent, rpcReferenceWrapper);
                }
            }
        }
        RESP_MAP.remove(rpcInfoContent.getUuid());
        throw new TimeoutException("Wait for response from server on client " + timeOut + "ms，retry times is " + retryTimes + ",service's name is " +
                rpcInfoContent.getTargetServiceName() + "#" + rpcInfoContent.getTargetMethod());
    }
}
