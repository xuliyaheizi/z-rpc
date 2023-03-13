package com.zhulin.test;

import com.zhulin.serializer.SerializeFactory;
import com.zhulin.serializer.impl.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/5
 * @Description:
 */
public class SerializFactoryTest {
    private static User buildRpcInfo() {
        User user=new User();
        user.setId(1);
        user.setName("zhfwafscawedqwdulin");
        user.setBankNo("2314141wafawdfwafawd2412512421");
        user.setAge(20);
        user.setSex(1);
        user.setRemark("hjdjskcvjakosjwfszacqwdsq   wdqwdqwdpdjqpoawdjk");
        return user;
    }

    @Benchmark
    public void fastJsonTest() {
        SerializeFactory serializeFactory = new FastJsonSerializeFactory();
        User rpcInfoContent = buildRpcInfo();
        byte[] serialize = serializeFactory.serialize(rpcInfoContent);
        User serialRpc = serializeFactory.deSerialize(User.class, serialize);
    }

    @Benchmark
    public void hessianTest() {
        SerializeFactory serializeFactory = new HessianSerializeFactory();
        User rpcInfoContent = buildRpcInfo();
        byte[] serialize = serializeFactory.serialize(rpcInfoContent);
        User serialRpc = serializeFactory.deSerialize(User.class, serialize);
    }

    @Benchmark
    public void jdkTest() {
        SerializeFactory serializeFactory = new JDKSerializeFactory();
        User rpcInfoContent = buildRpcInfo();
        byte[] serialize = serializeFactory.serialize(rpcInfoContent);
        User serialRpc = serializeFactory.deSerialize(User.class, serialize);
    }

    @Benchmark
    public void kryoTest() {
        SerializeFactory serializeFactory = new KryoSerializeFactory();
        User rpcInfoContent = buildRpcInfo();
        byte[] serialize = serializeFactory.serialize(rpcInfoContent);
        User serialRpc = serializeFactory.deSerialize(User.class, serialize);
    }

    @Benchmark
    public void protostuffTest() {
        SerializeFactory serializeFactory = new ProtostuffSerializeFactory();
        User rpcInfoContent = buildRpcInfo();
        byte[] serialize = serializeFactory.serialize(rpcInfoContent);
        User serialRpc = serializeFactory.deSerialize(User.class, serialize);
    }

    public static void main(String[] args) throws RunnerException {
        //配置进行2轮热数 测试2轮 1个线程
        //预热的原因 是JVM在代码执行多次会有优化
        Options options = new OptionsBuilder().warmupIterations(2).measurementBatchSize(2)
                .forks(1).build();
        new Runner(options).run();
    }

}
