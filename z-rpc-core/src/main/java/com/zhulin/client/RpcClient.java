package com.zhulin.client;

import com.alibaba.fastjson.JSON;
import com.zhulin.client.handler.ClientReadHandler;
import com.zhulin.client.reference.RpcReference;
import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import com.zhulin.commen.protocol.RpcProtocolCodec;
import com.zhulin.proxy.jdk.JDKProxyFactory;
import com.zhulin.services.UserService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.zhulin.commen.cache.CommonClientCache.*;

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
    public RpcReference initApplication() throws InterruptedException {
        worker = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
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
        ChannelFuture channelFuture = bootstrap.connect("localhost", 8080).sync();
        //开始发送请求
        this.startSendMsg(channelFuture);
        //获取代理对象
        return new RpcReference(new JDKProxyFactory());
    }

    /**
     * 开始发送请求
     *
     * @param channelFuture
     */
    public void startSendMsg(ChannelFuture channelFuture) {
        Thread thread = new Thread(new AsyncJob(channelFuture), "sendMsg");
        thread.start();
    }

    /**
     * 异步线程发送信息任务
     */
    class AsyncJob implements Runnable {
        private ChannelFuture channelFuture;

        public AsyncJob(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        @Override
        public void run() {
            try {
                //从消息队列中获取信息内容
                RpcInfoContent rpcInfoContent = SEND_QUEUE.take();
                //通过序列化方式将信息序列化为字节数组
                RpcProtocol rpcProtocol = new RpcProtocol(JSON.toJSONBytes(rpcInfoContent));
                //发送消息
                channelFuture.channel().writeAndFlush(rpcProtocol);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        RpcClient rpcClient = new RpcClient();
        RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
        rpcReferenceWrapper.setAimClass(UserService.class);
        RpcReference reference = rpcClient.initApplication();
        UserService userService = (UserService) reference.get(rpcReferenceWrapper);
        System.out.println(userService.sayHello("lisan"));
    }
}
