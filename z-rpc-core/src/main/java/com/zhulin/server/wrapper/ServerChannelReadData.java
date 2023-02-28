package com.zhulin.server.wrapper;

import com.zhulin.commen.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 服务端接收响应信息的包装类
 */
@Data
public class ServerChannelReadData {
    private RpcProtocol rpcProtocol;
    private ChannelHandlerContext ctx;
}
