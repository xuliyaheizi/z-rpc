package com.zhulin.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc客户端
 */
@Slf4j
public class RpcClient {
    private static NioEventLoopGroup worker;

    /**
     * 启动netty客户端
     */
    public void initApplication() {
        worker = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        //日志信息
                        ch.pipeline().addLast(new LoggingHandler());
                    }
                });
    }
}
