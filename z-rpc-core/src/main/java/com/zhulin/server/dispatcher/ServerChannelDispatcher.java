package com.zhulin.server.dispatcher;

import com.zhulin.commen.Exception.ZRpcException;
import com.zhulin.commen.concurrent.NamedThreadFactory;
import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.commen.protocol.RpcProtocol;
import com.zhulin.server.wrapper.ServerChannelReadData;

import java.lang.reflect.Method;
import java.util.concurrent.*;

import static com.zhulin.commen.cache.CommonServerCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description: 以多线程处理客户端的请求信息
 */
public class ServerChannelDispatcher {
    /**
     * 接收客户端请求信息的队列
     */
    private BlockingQueue<ServerChannelReadData> RPC_DATA_QUEUE;
    /**
     * 线程池
     */
    private ExecutorService executorService;

    /**
     * 初始化参数
     *
     * @param queueSize
     * @param bizThreadNums
     */
    public void init(int queueSize, int bizThreadNums) {
        //初始数据队列
        RPC_DATA_QUEUE = new ArrayBlockingQueue<>(queueSize);
        //初始线程池
        executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new NamedThreadFactory("zrpc", true));
    }

    /**
     * 添加数据
     *
     * @param serverChannelReadData
     */
    public void addData(ServerChannelReadData serverChannelReadData) {
        RPC_DATA_QUEUE.add(serverChannelReadData);
    }

    class ServerJobCoreHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    ServerChannelReadData channelReadData = RPC_DATA_QUEUE.take();
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            //获取客户端请求的协议体
                            RpcProtocol rpcProtocol = channelReadData.getRpcProtocol();
                            //反序列化得到请求内容
                            RpcInfoContent rpcInfoContent = SERVER_SERIALIZE_FACTORY.deSerialize(RpcInfoContent.class
                                    , rpcProtocol.getContent());
                            //执行过滤链路
                            try {
                                //执行前置过滤器
                                SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInfoContent);
                            } catch (Exception cause) {
                                //捕捉异常信息
                                if (cause instanceof ZRpcException) {
                                    ZRpcException zRpcException = (ZRpcException) cause;
                                    rpcInfoContent.setE(zRpcException);
                                    RpcProtocol respProtocol =
                                            new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
                                    channelReadData.getCtx().writeAndFlush(respProtocol);
                                    return;
                                }
                            }
                            //在服务端暴露的提供服务集合中通过服务名获取服务
                            Object aimClass = PROVIDER_MAP.get(rpcInfoContent.getTargetServiceName());
                            //获取该服务的方法
                            Method[] methods = aimClass.getClass().getDeclaredMethods();
                            Object result = null;
                            //遍历方法，反射执行目标方法
                            for (Method method : methods) {
                                if (method.getName().equals(rpcInfoContent.getTargetMethod())) {
                                    if (method.getReturnType().equals(Void.TYPE)) {
                                        try {
                                            method.invoke(aimClass, rpcInfoContent.getArgs());
                                        } catch (Exception e) {
                                            rpcInfoContent.setE(e);
                                        }
                                    } else {
                                        try {
                                            result = method.invoke(aimClass, rpcInfoContent.getArgs());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            rpcInfoContent.setE(e);
                                        }
                                    }
                                    //跳出循环
                                    break;
                                }
                            }
                            //写入响应数据
                            rpcInfoContent.setResponse(result);
                            //执行后置过滤器
                            SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInfoContent);
                            RpcProtocol respProtocol =
                                    new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInfoContent));
                            //给客户端发送响应数据
                            channelReadData.getCtx().writeAndFlush(respProtocol);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 开启多线程接收客户端请求数据
     */
    public void startDataConsumer() {
        Thread thread = new Thread(new ServerJobCoreHandler());
        thread.start();
    }
}
