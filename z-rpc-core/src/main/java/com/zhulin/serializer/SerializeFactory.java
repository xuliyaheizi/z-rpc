package com.zhulin.serializer;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 序列化工厂类
 */
public interface SerializeFactory {

    /**
     * 序列化
     *
     * @param t
     * @param <T>
     * @return
     */
    <T> byte[] serialize(T t);

    /**
     * 反序列化
     *
     * @param <T>
     * @param clazz
     * @param bytes
     * @return
     */
    <T> T deSerialize(Class<T> clazz, byte... bytes);
}
