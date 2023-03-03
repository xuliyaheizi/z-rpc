package com.zhulin.consumer.demo;

import com.zhulin.client.RpcClient;
import com.zhulin.client.handler.ConnectionHandler;
import com.zhulin.client.reference.RpcReference;
import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.interfaces.UserService;

import java.util.Scanner;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/1
 * @Description: 消费者功能测试
 */
public class ConsumerDemo {
    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        RpcClient rpcClient = new RpcClient();
        RpcReference reference = rpcClient.initApplication();
        //构建调用服务的请求信息
        RpcReferenceWrapper<UserService> rpcReferenceWrapper = new RpcReferenceWrapper<>();
        //field.getType()获取属性的类型
        rpcReferenceWrapper.setAimClass(UserService.class);
        rpcReferenceWrapper.setGroup("default");
        rpcReferenceWrapper.setRetry(0);
        rpcReferenceWrapper.setAsync(false);

        //订阅服务
        rpcClient.doSubscribeService(UserService.class);
        ConnectionHandler.setBootstrap(rpcClient.getBootstrap());
        rpcClient.doConnectServer();
        rpcClient.startSendMsg();

        UserService userService = reference.get(rpcReferenceWrapper);
        //System.out.println(userService.sayHello("zhulin"));

        while (true) {
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine();
            System.out.println(userService.sayHello(input));
        }

    }
}
