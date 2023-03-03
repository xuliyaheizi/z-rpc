package com.zhulin.server.handler;

import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import com.zhulin.server.wrapper.ServerChannelReadData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import static com.zhulin.commen.cache.CommonServerCache.SERVER_CHANNEL_DISPATCHER;
import static com.zhulin.commen.cache.CommonServerCache.SERVER_SERIALIZE_FACTORY;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 服务端读取数据处理器
 */
@Slf4j
public class ServerReadHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //服务端以统一的协议RpcProtocol接收数据
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        ServerChannelReadData serverChannelReadData = new ServerChannelReadData();
        serverChannelReadData.setRpcProtocol(rpcProtocol);
        serverChannelReadData.setCtx(ctx);
        SERVER_CHANNEL_DISPATCHER.addData(serverChannelReadData);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("{} 已经断开", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{} 已经异常断开，异常原因：{}", ctx.channel(), cause.getMessage());
    }
}
