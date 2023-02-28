package com.zhulin.server.handler;

import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

import static com.zhulin.commen.cache.CommonServerCache.PROVIDER_MAP;
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
        //将信息反序列化为RpcInfoContent
        RpcInfoContent rpcInfoContent = SERVER_SERIALIZE_FACTORY.deSerialize(RpcInfoContent.class,
                rpcProtocol.getContent());
        //在服务端暴露的提供服务集合中通过服务名获取服务
        Object aimClass = PROVIDER_MAP.get(rpcInfoContent.getTargetServiceName());
        //获取该服务的方法
        Method[] methods = aimClass.getClass().getDeclaredMethods();
        Object result = null;
        //遍历方法，反射执行目标方法
        for (Method method : methods) {
            if (method.getName().equals(rpcInfoContent.getTargetMethod())) {
                if (method.getReturnType().equals(Void.TYPE)) {
                    try {
                        method.invoke(aimClass, rpcInfoContent.getArgs());
                    } catch (Exception e) {
                        rpcInfoContent.setE(e);
                    }
                } else {
                    try {
                        result = method.invoke(aimClass, rpcInfoContent.getArgs());
                    } catch (Exception e) {
                        e.printStackTrace();
                        rpcInfoContent.setE(e);
                    }
                }
                //跳出循环
                break;
            }
        }
        //写入响应数据
        rpcInfoContent.setResponse(result);
        RpcProtocol respProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
        //给客户端发送响应数据
        ctx.writeAndFlush(respProtocol);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("{} 已经断开", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("{} 已经异常断开，异常原因：{}", ctx.channel(), cause.getMessage());
        cause.printStackTrace();
    }
}
