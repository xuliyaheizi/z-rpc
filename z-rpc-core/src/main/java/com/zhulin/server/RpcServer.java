package com.zhulin.server;

import com.zhulin.commen.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc服务端
 */
@Slf4j
public class RpcServer {
    private static NioEventLoopGroup boss = null;
    private static NioEventLoopGroup workers = null;
    private static ServerConfig serverConfig;

    /**
     * 启动netty服务端
     */
    public void startApplication() throws InterruptedException {
        boss = new NioEventLoopGroup();
        workers = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                //有数据立马发送
                .option(ChannelOption.TCP_NODELAY, true)
                //
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<NioServerSocketChannel>() {
                    @Override
                    protected void initChannel(NioServerSocketChannel ch) throws Exception {
                        //服务端日志信息
                        ch.pipeline().addLast(new LoggingHandler());
                    }
                });
        //netty服务端绑定端口号
        bootstrap.bind(serverConfig.getServerPort()).sync();
    }
}
