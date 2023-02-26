package com.zhulin.server.handler;

import com.alibaba.fastjson.JSON;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;

import static com.zhulin.commen.cache.CommonServerCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 服务端读取数据处理器
 */
public class ServerReadHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        //服务端以统一的协议RpcProtocol接收数据
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] content = rpcProtocol.getContent();
        //将信息反序列化为RpcInfoContent
        RpcInfoContent rpcInfoContent = JSON.parseObject(new String(content), RpcInfoContent.class);
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
                        rpcInfoContent.setE(e);
                    }
                }
                //跳出循环
                break;
            }
        }
        //写入响应数据
        rpcInfoContent.setResponse(result);
        RpcProtocol respProtocol = new RpcProtocol(JSON.toJSONBytes(rpcInfoContent));
        //给客户端发送响应数据
        ctx.writeAndFlush(respProtocol);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
