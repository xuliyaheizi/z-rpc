package com.zhulin.serializer.impl;

import com.zhulin.serializer.SerializeFactory;

import java.io.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/19
 * @Description: JDK序列化方式
 */
public class JDKSerializeFactory implements SerializeFactory {
    @Override
    public <T> byte[] serialize(T t) {
        byte[] data = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(os);
            output.writeObject(t);
            output.flush();
            output.close();
            data = os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("JDK序列化失败", e);
        }
        return data;
    }

    @Override
    public <T> T deSerialize(Class<T> clazz, byte... bytes) {
        T outData = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            outData = (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("JDK反序列化失败", e);
        }
        return outData;
    }

}
