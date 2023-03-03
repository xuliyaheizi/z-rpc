package com.zhulin.proxy.jdk;

import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.commen.constants.RpcConstants;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.concurrent.TimeoutInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.zhulin.commen.cache.CommonClientCache.*;

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
    private Integer timeOut = CLIENT_CONFIG.getTimeOut() == null ? RpcConstants.DEFAULT_TIMEOUT :
            CLIENT_CONFIG.getTimeOut();

    public JDKInvocationHandler(RpcReferenceWrapper rpcReferenceWrapper) {
        this.rpcReferenceWrapper = rpcReferenceWrapper;
    }

    //重试次数
    private int retryTimes = 0;

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
        if (rpcReferenceWrapper.isAsync()) {
            //不需要获取响应数据，只需要向服务端发起一步请求即可
            //将传输内容添加到消息队列中
            SEND_QUEUE.add(rpcInfoContent);
            return null;
        }
        //将该请求保存在响应集合中
        TimeoutInvocation timeoutInvocation = new TimeoutInvocation(null);
        RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
        //将传输内容添加到消息队列中
        SEND_QUEUE.add(rpcInfoContent);
        //判断是否超时 或者 是否设置了重试机制
        Boolean isNotTimeOut = timeoutInvocation.tryAcquire(timeOut, TimeUnit.MILLISECONDS);
        if (isNotTimeOut || rpcInfoContent.getRetry() > 0) {
            RpcInfoContent respInfo =
                    ((TimeoutInvocation) RESP_MAP.remove(rpcInfoContent.getUuid())).getRpcInfoContent();
            if (isNotTimeOut && respInfo.getE() == null) {
                //为超时且没有异常信息
                return respInfo.getResponse();
            } else if (respInfo.getRetry() > 0) {
                //重试次数++
                retryTimes++;
                //重试，重新设置UUID
                //rpcInfoContent.setUuid(UUID.randomUUID().toString());
                respInfo.setResponse(null);
                respInfo.setE(null);
                respInfo.setRetry(respInfo.getRetry() - 1);
                //回调接收重试之后的响应数据
                return tryFinishedTask(respInfo, rpcReferenceWrapper);
            } else if (respInfo.getE() != null) {
                respInfo.getE().printStackTrace();
            }
        }
        RESP_MAP.remove(rpcInfoContent.getUuid());
        throw new TimeoutException("Wait for response from server on client " + timeOut + "ms，retry times is " + retryTimes + ",service's name is " +
                rpcInfoContent.getTargetServiceName() + "#" + rpcInfoContent.getTargetMethod());
    }
}
