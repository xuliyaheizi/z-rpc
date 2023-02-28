package com.zhulin.filter.client.impl;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.concurrent.TimeoutInvocation;
import com.zhulin.filter.ZClientFilter;

import java.util.Iterator;
import java.util.List;

import static com.zhulin.commen.cache.CommonClientCache.RESP_MAP;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: IP直连过滤器
 */
public class DirectInvokeFilterImpl implements ZClientFilter {
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInfoContent rpcInfoContent) {
        String url = (String) rpcInfoContent.getAttachments().get("url");
        if (CommonUtil.isEmpty(url)) {
            return;
        }
        //遍历已连接的服务提供者，筛选出直连IP的服务提供者
        Iterator<ChannelFutureWrapper> iterator = src.iterator();
        while (iterator.hasNext()) {
            ChannelFutureWrapper channelFutureWrapper = iterator.next();
            if (!(channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort()).equals(url)) {
                iterator.remove();
            }
        }
        //该IP没有提供该服务
        if (CommonUtil.isEmptyList(src)) {
            rpcInfoContent.setRetry(0);
            rpcInfoContent.setE(new RuntimeException("no provider match for service " + rpcInfoContent.getTargetServiceName() + " in url " + url));
            rpcInfoContent.setResponse(null);
            //直接交给响应线程那边处理（响应线程在代理类内部的invoke函数中，那边会取出对应的uuid的值，然后判断）
            TimeoutInvocation timeoutInvocation = (TimeoutInvocation) RESP_MAP.get(rpcInfoContent.getUuid());
            timeoutInvocation.setRpcInfoContent(rpcInfoContent);
            RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
            //通知代理类中的响应线程
            timeoutInvocation.release();
            throw new RuntimeException("no provider match for service " + rpcInfoContent.getTargetServiceName() + " " +
                    "in" + " url " + url);
        }
    }
}
