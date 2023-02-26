package com.zhulin.client.handler;

import com.alibaba.fastjson.JSON;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import com.zhulin.concurrent.TimeoutInvocation;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static com.zhulin.commen.cache.CommonClientCache.RESP_MAP;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 响应数据接收处理器
 */
public class ClientReadHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //获取接收信息
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] content = rpcProtocol.getContent();
        //将字节数组反序列化为RpcInfoContent
        RpcInfoContent rpcInfoContent = JSON.parseObject(new String(content), RpcInfoContent.class);
        //判断是否有异常信息
        if (rpcInfoContent.getE() != null) {
            rpcInfoContent.getE().printStackTrace();
        }
        if (!RESP_MAP.containsKey(rpcInfoContent.getUuid())) {
            throw new IllegalArgumentException("server response is error");
        }
        //将请求的响应结构放入一个Map集合中，集合的key就是uuid，这个uuid在发送请求之前就已经初始化好了，所以只需要起一个线程在后台遍历这个map，查看对应的key是否有相应即可。
        TimeoutInvocation timeoutInvocation = (TimeoutInvocation) RESP_MAP.get(rpcInfoContent.getUuid());
        timeoutInvocation.setRpcInfoContent(rpcInfoContent);
        RESP_MAP.put(rpcInfoContent.getUuid(), timeoutInvocation);
        timeoutInvocation.release();
    }
}
