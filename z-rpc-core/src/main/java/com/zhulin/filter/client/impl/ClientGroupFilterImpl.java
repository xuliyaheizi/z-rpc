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
 * @Description: 基于分组的过滤策略
 */
public class ClientGroupFilterImpl implements ZClientFilter {
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInfoContent rpcInfoContent) {
        //获取客户端发起请求的组名
        String group = String.valueOf(rpcInfoContent.getAttachments().get("group"));
        //遍历已连接的服务提供者，筛选出组名一致的服务提供者
        Iterator<ChannelFutureWrapper> channelFutureWrapperIterator = src.iterator();
        while (channelFutureWrapperIterator.hasNext()) {
            ChannelFutureWrapper channelFutureWrapper = channelFutureWrapperIterator.next();
            if (!channelFutureWrapper.getGroup().equals(group)) {
                src.remove(channelFutureWrapper);
            }
        }
        //当筛选后，没有服务提供者时
        if (CommonUtil.isEmptyList(src)) {
            //设置请求的重试次数为0
            rpcInfoContent.setRetry(0);
            rpcInfoContent.setE(new RuntimeException("no provider match for service " + rpcInfoContent.getTargetServiceName() + " in group " + group));
            rpcInfoContent.setResponse(null);
            //直接交给响应线程那边处理（响应线程在代理类内部的invoke函数中，那边会取出对应的uuid的值，然后判断）
            TimeoutInvocation timeoutInvocation = (TimeoutInvocation) RESP_MAP.get(rpcInfoContent.getUuid());
            timeoutInvocation.setRpcInfoContent(rpcInfoContent);
            RESP_MAP.put(rpcInfoContent.getUuid(), rpcInfoContent);
            //通知代理类中的接收响应信息线程
            timeoutInvocation.release();
            throw new RuntimeException("no provider match for service " + rpcInfoContent.getTargetServiceName() + " " +
                    "in group " + group);
        }
    }
}
