package com.zhulin.serializer;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 序列化工厂类
 */
public interface SerializerFactory {

    /**
     * 序列化
     *
     * @param t
     * @param <T>
     * @return
     */
    <T> byte[] serializer(T t);

    /**
     * 反序列化
     *
     * @param clazz
     * @param bytes
     * @param <T>
     * @return
     */
    <T> T deSerializer(Class<T> clazz, byte[] bytes);
}
