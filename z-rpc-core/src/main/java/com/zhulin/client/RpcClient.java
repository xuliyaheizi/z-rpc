package com.zhulin.client;

import com.alibaba.fastjson.JSON;
import com.zhulin.client.handler.ClientReadHandler;
import com.zhulin.client.handler.ConnectionHandler;
import com.zhulin.client.reference.RpcReference;
import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.commen.config.PropertiesBootstrap;
import com.zhulin.commen.constants.RpcConstants;
import com.zhulin.commen.event.ZRpcListenerLoader;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.filter.ZClientFilter;
import com.zhulin.filter.client.ClientFilterChain;
import com.zhulin.proxy.jdk.JDKProxyFactory;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.RegistryService;
import com.zhulin.registry.URL;
import com.zhulin.router.ZRouter;
import com.zhulin.serializer.SerializeFactory;
import com.zhulin.services.UserService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER;
import static com.zhulin.commen.cache.CommonCache.EXTENSION_LOADER_CLASS_CACHE;
import static com.zhulin.commen.cache.CommonClientCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc客户端
 */
@Slf4j
@Data
public class RpcClient {

    private static NioEventLoopGroup worker;

    private Bootstrap bootstrap = new Bootstrap();
    private static ZRpcListenerLoader zRpcListenerLoader;

    /**
     * 启动netty客户端
     */
    public RpcReference initApplication() throws InstantiationException, IllegalAccessException {
        //初始配置信息
        this.initClientConfig();
        worker = new NioEventLoopGroup();
        bootstrap.group(worker).channel(NioSocketChannel.class).handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ByteBuf delimiter = Unpooled.copiedBuffer(RpcConstants.DEFAULT_DECODE_CHAR.getBytes());
                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(CLIENT_CONFIG.getMaxServerRespDataSize(),
                        delimiter));
                //日志信息
                ch.pipeline().addLast(new LoggingHandler());
                //协议体解编码器
                ch.pipeline().addLast(new RpcProtocolCodec());
                //获取响应数据
                ch.pipeline().addLast(new ClientReadHandler());
            }
        });
        //初始化事件监听器
        zRpcListenerLoader = new ZRpcListenerLoader();
        zRpcListenerLoader.init();
        //获取代理对象
        return new RpcReference(new JDKProxyFactory());
    }

    /**
     * 开始发送请求
     */
    public void startSendMsg() {
        Thread thread = new Thread(new AsyncJob(), "sendMsgTask");
        thread.start();
    }

    /**
     * 异步线程发送信息任务
     */
    class AsyncJob implements Runnable {

        public AsyncJob() {

        }

        @Override
        public void run() {
            try {
                //从消息队列中获取信息内容
                RpcInfoContent rpcInfoContent = SEND_QUEUE.take();
                //通过序列化方式将信息序列化为字节数组
                RpcProtocol rpcProtocol = new RpcProtocol(CLIENT_SERIALIZE_FACTORY.serialize(rpcInfoContent));
                //发送消息
                ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(rpcInfoContent);
                channelFuture.channel().writeAndFlush(rpcProtocol);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始客户端基本服务
     */
    public void initClientConfig() throws InstantiationException, IllegalAccessException {
        //初始化客户端配置信息
        CLIENT_CONFIG = PropertiesBootstrap.loadClientConfigFromLocal();
        //初始化客户端序列化方式
        CLIENT_SERIALIZE_FACTORY = EXTENSION_LOADER.exampleClass(SerializeFactory.class,
                CLIENT_CONFIG.getClientSerialize());
        //初始负载均衡策略
        ZROUTER = EXTENSION_LOADER.exampleClass(ZRouter.class, CLIENT_CONFIG.getRouterStrategy());
        //初始化过滤链 SPI配置文件中指定过滤的顺序
        EXTENSION_LOADER.loadExtension(ZClientFilter.class);
        ClientFilterChain clientFilterChain = new ClientFilterChain();
        LinkedHashMap<String, Class> clientFilterMap =
                EXTENSION_LOADER_CLASS_CACHE.get(ZClientFilter.class.getName());
        for (String implClassName : clientFilterMap.keySet()) {
            Class aClass = clientFilterMap.get(implClassName);
            if (aClass == null) {
                throw new RuntimeException("no match zClientFilter for " + ZClientFilter.class.getName());
            }
            clientFilterChain.addZClientFilter((ZClientFilter) aClass.newInstance());
        }
        CLIENT_FILTER_CHAIN = clientFilterChain;
    }

    /**
     * 启动服务之前需要预先订阅对应的服务
     *
     * @param serviceBean
     */
    public void doSubscribeService(Class serviceBean) {
        if (REGISTRY_SERVICE == null) {
            REGISTRY_SERVICE = (AbstractRegistry) EXTENSION_LOADER.exampleClass(RegistryService.class,
                    CLIENT_CONFIG.getRegisterType());
        }
        //构建订阅信息
        URL url = new URL();
        url.setApplicationName(CLIENT_CONFIG.getApplicationName());
        url.setServiceName(serviceBean.getName());
        url.addParameter("host", CommonUtil.getIpAddress());
        //获取注册中心的该服务的权重信息
        Map<String, String> result = REGISTRY_SERVICE.getProviderNodeInfos(serviceBean.getName());
        URL_MAP.put(serviceBean.getName(), result);
        //向注册中心发起订阅
        REGISTRY_SERVICE.subscriber(url);
    }

    /**
     * 开始和各个provider建立连接，同时监听各个providerNode节点的变化（child变化和nodeData的变化）
     */
    public void doConnectServer() {
        //与订阅服务的提供者连接
        for (URL providerUrl : SUBSCRIBER_SERVICE_LIST) {
            //获取该服务的所有提供者IP地址
            List<String> providerIps = REGISTRY_SERVICE.getProviderIps(providerUrl.getServiceName());
            for (String providerIp : providerIps) {
                try {
                    ConnectionHandler.connect(providerUrl.getServiceName(), providerIp);
                } catch (InterruptedException e) {
                    log.error("[doConnectServer] connect fail ", e);
                }
            }
            //监听服务提供者的变化
            URL url = new URL();
            //servicePath ---> com.zhulin.services.UserService/provider
            url.addParameter("servicePath", providerUrl.getServiceName() + "/provider");
            url.addParameter("providerIps", JSON.toJSONString(providerIps));
            REGISTRY_SERVICE.doAfterSubscribe(url);
        }
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        RpcClient rpcClient = new RpcClient();
        RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
        rpcReferenceWrapper.setAimClass(UserService.class);
        rpcReferenceWrapper.setGroup("dev");
        rpcReferenceWrapper.setServiceToken("dawdwa");
        RpcReference reference = rpcClient.initApplication();
        //订阅服务
        rpcClient.doSubscribeService(UserService.class);
        //连接服务
        ConnectionHandler.bootstrap = rpcClient.getBootstrap();
        rpcClient.doConnectServer();
        rpcClient.startSendMsg();
        UserService userService = (UserService) reference.get(rpcReferenceWrapper);
        System.out.println(userService.sayHello("zhulin"));
    }
}
