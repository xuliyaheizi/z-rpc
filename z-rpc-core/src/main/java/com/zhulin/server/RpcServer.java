package com.zhulin.server;

import com.zhulin.commen.config.PropertiesBootstrap;
import com.zhulin.commen.event.ZRpcListenerLoader;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.RegistryService;
import com.zhulin.registry.URL;
import com.zhulin.serializer.SerializeFactory;
import com.zhulin.server.handler.RpcServiceWrapper;
import com.zhulin.server.handler.ServerReadHandler;
import com.zhulin.server.handler.ServerShutDownHook;
import com.zhulin.services.impl.UserServiceImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER;
import static com.zhulin.commen.cache.CommonServerCache.*;
import static com.zhulin.commen.constants.RpcConstants.DEFAULT_DECODE_CHAR;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc服务端
 */
@Slf4j
public class RpcServer {
    private static NioEventLoopGroup boss = null;
    private static NioEventLoopGroup workers = null;

    /**
     * 启动netty服务端
     */
    public void startApplication() throws InterruptedException {
        boss = new NioEventLoopGroup();
        workers = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, workers).channel(NioServerSocketChannel.class)
                //有数据立马发送
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ByteBuf delimiter = Unpooled.copiedBuffer(DEFAULT_DECODE_CHAR.getBytes());
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(SERVER_CONFIG.getMaxServerRequestData(),
                                delimiter));
                        //服务端日志信息
                        ch.pipeline().addLast(new LoggingHandler());
                        //协议体解码器
                        ch.pipeline().addLast(new RpcProtocolCodec());
                        //客户端信息处理器
                        ch.pipeline().addLast(new ServerReadHandler());
                    }
                });
        //批量注册服务
        this.batchRegistryUrl();
        //netty服务端绑定端口号
        bootstrap.bind(SERVER_CONFIG.getServerPort()).sync();
        log.info("[startApplication] server is started!");
    }

    /**
     * 初始化基本服务
     */
    public void initServerConfig() {
        //初始服务端配置信息
        SERVER_CONFIG = PropertiesBootstrap.loadServiceConfigFormLocal();
        //初始化注册中心
        REGISTRY_SERVICE = (AbstractRegistry) EXTENSION_LOADER.exampleClass(RegistryService.class,
                SERVER_CONFIG.getRegisterType());
        //初始化服务端序列化方式
        SERVER_SERIALIZE_FACTORY = EXTENSION_LOADER.exampleClass(SerializeFactory.class,
                SERVER_CONFIG.getServerSerialize());
    }

    /**
     * 服务注册
     *
     * @param rpcServiceWrapper
     */
    public void registryService(RpcServiceWrapper rpcServiceWrapper) {
        Object serviceBean = rpcServiceWrapper.getServiceObj();
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
        //构建服务注册信息
        URL url = new URL();
        url.setApplicationName(SERVER_CONFIG.getApplicationName());
        url.setServiceName(interfaceClass.getName());
        url.addParameter("host", CommonUtil.getIpAddress());
        url.addParameter("port", String.valueOf(SERVER_CONFIG.getServerPort()));
        url.addParameter("group", String.valueOf(rpcServiceWrapper.getGroup()));
        PROVIDER_URL_SET.add(url);
    }

    /**
     * 批量注册服务
     */
    private void batchRegistryUrl() {
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (URL url : PROVIDER_URL_SET) {
                    REGISTRY_SERVICE.register(url);
                    log.info("[Server] export service {}", url.getServiceName());
                }
            }
        }, "registryServerTask");
        task.start();
    }

    public static void main(String[] args) throws InterruptedException {
        RpcServer rpcServer = new RpcServer();
        rpcServer.initServerConfig();
        //事件监听机制
        ZRpcListenerLoader iRpcListenerLoader = new ZRpcListenerLoader();
        iRpcListenerLoader.init();
        RpcServiceWrapper rpcServiceWrapper = new RpcServiceWrapper(new UserServiceImpl(), "dev");
        rpcServer.registryService(rpcServiceWrapper);
        rpcServer.startApplication();
        ServerShutDownHook.registryShutdownHook();
    }
}
