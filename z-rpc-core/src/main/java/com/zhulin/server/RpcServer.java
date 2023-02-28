package com.zhulin.server;

import com.zhulin.commen.annotations.SPI;
import com.zhulin.commen.config.PropertiesBootstrap;
import com.zhulin.commen.event.ZRpcListenerLoader;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.filter.ZServerFilter;
import com.zhulin.filter.server.ServerAfterFilterChain;
import com.zhulin.filter.server.ServerBeforeFilterChain;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.RegistryService;
import com.zhulin.registry.URL;
import com.zhulin.serializer.SerializeFactory;
import com.zhulin.server.handler.MaxConnectionLimitHandler;
import com.zhulin.server.wrapper.RpcServiceWrapper;
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

import java.util.LinkedHashMap;

import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER;
import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER_CLASS_CACHE;
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
        //test
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, workers).channel(NioServerSocketChannel.class)
                //有数据立马发送
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                //服务端采用单一长连接的模式，这里所支持的最大连接数应该和机器本身的性能有关
                //连接防护的handler应该绑定在Main-Reactor上
                .handler(new MaxConnectionLimitHandler(SERVER_CONFIG.getMaxConnections()))
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
                        //客户端信息处理器 这里面需要注意出现堵塞的情况发生，建议将核心业务内容分配给业务线程池处理
                        ch.pipeline().addLast(new ServerReadHandler());
                    }
                });
        //批量注册服务
        this.batchRegistryUrl();
        //开始准备接收请求任务
        SERVER_CHANNEL_DISPATCHER.statDataConsumer();
        //netty服务端绑定端口号
        bootstrap.bind(SERVER_CONFIG.getServerPort()).sync();
        log.info("[startApplication] server is started!");
    }

    /**
     * 初始化基本服务
     */
    public void initServerConfig() throws InstantiationException, IllegalAccessException {
        //初始服务端配置信息
        SERVER_CONFIG = PropertiesBootstrap.loadServiceConfigFormLocal();
        //初始服务端端执行读取请求数据的线程池和队列参数
        SERVER_CHANNEL_DISPATCHER.init(SERVER_CONFIG.getServerQueueSize(), SERVER_CONFIG.getServerBizThreadNums());
        //初始化注册中心
        REGISTRY_SERVICE = (AbstractRegistry) EXTENSION_LOADER.exampleClass(RegistryService.class,
                SERVER_CONFIG.getRegisterType());
        //初始化服务端序列化方式
        SERVER_SERIALIZE_FACTORY = EXTENSION_LOADER.exampleClass(SerializeFactory.class,
                SERVER_CONFIG.getServerSerialize());
        //初始化服务端过滤链
        EXTENSION_LOADER.loadExtension(ZServerFilter.class);
        ServerBeforeFilterChain serverBeforeFilterChain = new ServerBeforeFilterChain();
        ServerAfterFilterChain serverAfterFilterChain = new ServerAfterFilterChain();
        LinkedHashMap<String, Class> serverFilterMap =
                EXTENSION_LOADER_CLASS_CACHE.get(ZServerFilter.class.getName());
        for (String serverFilterName : serverFilterMap.keySet()) {
            Class aClass = serverFilterMap.get(serverFilterName);
            //获取实现类的注解信息
            SPI spi = (SPI) aClass.getDeclaredAnnotation(SPI.class);
            //根据注解信息，判断是前置过滤器还是后置过滤器
            if (spi != null && "before".equals(spi.value())) {
                serverBeforeFilterChain.addZServerFilter((ZServerFilter) aClass.newInstance());
            } else if (spi != null && "after".equals(spi.value())) {
                serverAfterFilterChain.addZServerFilter((ZServerFilter) aClass.newInstance());
            }
        }
        SERVER_BEFORE_FILTER_CHAIN = serverBeforeFilterChain;
        SERVER_AFTER_FILTER_CHAIN = serverAfterFilterChain;
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

    public static void main(String[] args) throws InterruptedException, InstantiationException, IllegalAccessException {
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
