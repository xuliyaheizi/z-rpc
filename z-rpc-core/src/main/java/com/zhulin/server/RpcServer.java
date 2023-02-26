package com.zhulin.server;

import com.zhulin.commen.config.ServerConfig;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.server.handler.ServerReadHandler;
import com.zhulin.services.impl.UserServiceImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.zhulin.commen.cache.CommonServerCache.PROVIDER_MAP;

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
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        //服务端日志信息
                        ch.pipeline().addLast(new LoggingHandler());
                        //协议体解码器
                        ch.pipeline().addLast(new RpcProtocolCodec());
                        //客户端信息处理器
                        ch.pipeline().addLast(new ServerReadHandler());
                    }
                });
        //netty服务端绑定端口号
        bootstrap.bind(8080).sync();
    }

    /**
     * 服务注册
     *
     * @param serviceBean
     */
    public void registyService(Object serviceBean) {
        if (serviceBean.getClass().getInterfaces().length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        Class[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        Class interfaceClass = classes[0];
        //需要注册的对象统一放在一个MAP集合中进行管理
        PROVIDER_MAP.put(interfaceClass.getName(), serviceBean);
    }



    public static void main(String[] args) throws InterruptedException {
        RpcServer rpcServer = new RpcServer();
        rpcServer.registyService(new UserServiceImpl());
        rpcServer.startApplication();
    }
}
