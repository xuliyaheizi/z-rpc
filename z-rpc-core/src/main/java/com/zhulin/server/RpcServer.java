package com.zhulin.server;

import com.zhulin.commen.annotations.SPI;
import com.zhulin.commen.concurrent.ServiceSemaphoreWrapper;
import com.zhulin.commen.config.PropertiesBootstrap;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.commen.protocol.RpcProtocolFrameDecoder;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.commen.utils.ThreadPoolUtil;
import com.zhulin.filter.ZServerFilter;
import com.zhulin.filter.server.ServerAfterFilterChain;
import com.zhulin.filter.server.ServerBeforeFilterChain;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.RegistryService;
import com.zhulin.registry.URL;
import com.zhulin.serializer.SerializeFactory;
import com.zhulin.server.handler.MaxConnectionLimitHandler;
import com.zhulin.server.handler.ServerReadHandler;
import com.zhulin.server.wrapper.RpcServiceWrapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER;
import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER_CLASS_CACHE;
import static com.zhulin.commen.cache.CommonServerCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc服务端
 */
@Slf4j
public class RpcServer {

    private Thread thread;

    /**
     * 启动netty服务端
     */
    public void startApplication() {

        thread = new Thread(() -> {
            final ThreadPoolExecutor threadPoolExecutor =
                    ThreadPoolUtil.makeServerThreadPool(RpcServer.class.getName(), 0, Integer.MAX_VALUE);
            EventLoopGroup boss = new NioEventLoopGroup();
            EventLoopGroup workers = new NioEventLoopGroup();

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                //设置主从线程池
                bootstrap.group(boss, workers).channel(NioServerSocketChannel.class)
                        //有数据立马发送
                        .option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true)
                        //服务端采用单一长连接的模式，这里所支持的最大连接数应该和机器本身的性能有关
                        //连接防护的handler应该绑定在Main-Reactor上
                        .handler(new MaxConnectionLimitHandler(SERVER_CONFIG.getMaxConnections())).childHandler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel ch) {
                                //协议头解码器
                                ch.pipeline().addLast(new RpcProtocolFrameDecoder());
                                //协议体解码器
                                ch.pipeline().addLast(new RpcProtocolCodec());
                                //客户端信息处理器 这里面需要注意出现堵塞的情况发生，建议将核心业务内容分配给业务线程池处理
                                ch.pipeline().addLast(new ServerReadHandler());
                            }
                        });
                //netty服务端绑定端口号
                ChannelFuture channelFuture = bootstrap.bind(SERVER_CONFIG.getServerPort()).sync();
                //批量注册服务
                batchRegistryUrl();
                //开始准备接收请求任务
                SERVER_CHANNEL_DISPATCHER.startDataConsumer(SERVER_CONFIG.getServerQueueSize(), threadPoolExecutor);
                //等待关闭
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    log.info(">>>>>>>>>>> z-rpc netty server stop.");
                } else {
                    log.error(">>>>>>>>>>> z-rpc netty server error.", e);
                }
            } finally {
                threadPoolExecutor.shutdown();
                boss.shutdownGracefully();
                workers.shutdownGracefully();
            }
        }, "nettyServer");

        //设置守护线程
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 初始化基本服务
     */
    public void initServerConfig() throws InstantiationException, IllegalAccessException {
        //初始服务端配置信息
        SERVER_CONFIG = PropertiesBootstrap.loadServiceConfigFormLocal();
        //初始化服务端序列化方式
        SERVER_SERIALIZE_FACTORY = EXTENSION_LOADER.exampleClass(SerializeFactory.class,
                SERVER_CONFIG.getServerSerialize());
        //初始化服务端过滤链
        EXTENSION_LOADER.loadExtension(ZServerFilter.class);
        ServerBeforeFilterChain serverBeforeFilterChain = new ServerBeforeFilterChain();
        ServerAfterFilterChain serverAfterFilterChain = new ServerAfterFilterChain();
        LinkedHashMap<String, Class> serverFilterMap = EXTENSION_LOADER_CLASS_CACHE.get(ZServerFilter.class.getName());
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
     * @param rpcServiceWrapper 服务注册信息
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
        url.addParameter("limit", String.valueOf(rpcServiceWrapper.getLimit()));
        //设置服务端的限流器
        SERVER_SERVICE_SEMAPHORE.put(interfaceClass.getName(),
                new ServiceSemaphoreWrapper(rpcServiceWrapper.getLimit()));
        PROVIDER_URL_SET.add(url);
        //判断服务是否需要权限校验
        if (!CommonUtil.isEmpty(rpcServiceWrapper.getServiceToken())) {
            PROVIDER_SERVICE_WRAPPER_MAP.put(interfaceClass.getName(), rpcServiceWrapper);
        }
    }

    /**
     * 批量注册服务
     */
    private void batchRegistryUrl() {
        if (REGISTRY_SERVICE == null) {
            //初始化注册中心
            REGISTRY_SERVICE = (AbstractRegistry) EXTENSION_LOADER.exampleClass(RegistryService.class,
                    SERVER_CONFIG.getRegisterType());
        }
        for (URL url : PROVIDER_URL_SET) {
            REGISTRY_SERVICE.register(url);
        }
    }
}
