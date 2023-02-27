package com.zhulin.client;

import com.alibaba.fastjson.JSON;
import com.zhulin.client.handler.ClientReadHandler;
import com.zhulin.client.handler.ConnectionHandler;
import com.zhulin.client.reference.RpcReference;
import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.commen.config.PropertiesBootstrap;
import com.zhulin.commen.event.ZRpcListenerLoader;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.commen.utils.CommonUtil;
import com.zhulin.proxy.jdk.JDKProxyFactory;
import com.zhulin.registry.AbstractRegistry;
import com.zhulin.registry.URL;
import com.zhulin.registry.zookeeper.impl.ZookeeperRegistry;
import com.zhulin.router.impl.RandomRouterImpl;
import com.zhulin.services.UserService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

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
    private static AbstractRegistry REGISTRY_SERVICE;
    private Bootstrap bootstrap = new Bootstrap();
    private static ZRpcListenerLoader zRpcListenerLoader;

    /**
     * 启动netty客户端
     */
    public RpcReference initApplication() throws InterruptedException {
        //初始配置信息
        this.initClientConfig();
        worker = new NioEventLoopGroup();
        bootstrap.group(worker).channel(NioSocketChannel.class).handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
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
                RpcProtocol rpcProtocol = new RpcProtocol(JSON.toJSONBytes(rpcInfoContent));
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
    public void initClientConfig() {
        //初始化客户端配置信息
        CLIENT_CONFIG = PropertiesBootstrap.loadClientConfigFromLocal();
        //初始负载均衡策略
        ZROUTER = new RandomRouterImpl();
    }

    /**
     * 启动服务之前需要预先订阅对应的服务
     *
     * @param serviceBean
     */
    public void doSubscribeService(Class serviceBean) {
        if (REGISTRY_SERVICE == null) {
            REGISTRY_SERVICE = new ZookeeperRegistry(CLIENT_CONFIG.getRegisterAddr());
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
        for (URL providerUrl : SUBSCRIBER_SERVICE_LIST) {
            List<String> providerIps = REGISTRY_SERVICE.getProviderIps(providerUrl.getServiceName());
            for (String providerIp : providerIps) {
                try {
                    ConnectionHandler.connect(providerUrl.getServiceName(), providerIp);
                } catch (InterruptedException e) {
                    log.error("[doConnectServer] connect fail ", e);
                }
            }
            URL url = new URL();
            //servicePath ---> com.zhulin.services.UserService/provider
            url.addParameter("servicePath", providerUrl.getServiceName() + "/provider");
            url.addParameter("providerIps", JSON.toJSONString(providerIps));
            //客户端在此新增一个订阅功能
            REGISTRY_SERVICE.doAfterSubscribe(url);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RpcClient rpcClient = new RpcClient();
        RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
        rpcReferenceWrapper.setAimClass(UserService.class);
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
