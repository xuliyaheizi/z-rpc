package com.zhulin.test;

import com.zhulin.commen.protocol.RpcInfoContent;
import com.zhulin.interfaces.UserService;
import com.zhulin.serializer.impl.KryoSerializeFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/5
 * @Description:
 */
public class GzipDemoTest {

    @Test
    public void gzipDemoTest() throws NoSuchMethodException {
        RpcInfoContent rpcInfoContent = new RpcInfoContent();
        rpcInfoContent.setUuid(UUID.randomUUID().toString());
        rpcInfoContent.setTargetServiceName(UserService.class.getName());
        rpcInfoContent.setTargetMethod(UserService.class.getMethod("sayHello", String.class).getName());
        rpcInfoContent.setArgs(new Object[]{"zhulin"});
        Map<String,Object> attachments=new HashMap<>();
        attachments.put("group", "dev");
        rpcInfoContent.setAttachments(attachments);

        KryoSerializeFactory kryoSerializeFactory = new KryoSerializeFactory();
        byte[] serialize = kryoSerializeFactory.serialize(rpcInfoContent);
        RpcInfoContent rpcInfoContent1 = kryoSerializeFactory.deSerialize(RpcInfoContent.class, serialize);

        System.out.println(rpcInfoContent1);
    }
}
