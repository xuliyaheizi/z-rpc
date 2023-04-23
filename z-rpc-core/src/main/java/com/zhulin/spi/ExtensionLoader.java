package com.zhulin.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;

import static com.zhulin.commen.cache.CommonCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 自定义SPI机制
 */
public class  ExtensionLoader {

    private static final String EXTENSION_LOADER_DIR_PREFIX = "META-INF/zrpc/";

    /**
     * 通过配置文件去加载相关类
     *
     * @param clazz
     */
    public void loadExtension(Class clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("class is null");
        }
        try {
            String spiFilePath = EXTENSION_LOADER_DIR_PREFIX + clazz.getName();
            ClassLoader classLoader = this.getClass().getClassLoader();
            Enumeration<URL> enumeration = null;
            enumeration = classLoader.getResources(spiFilePath);
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                InputStreamReader inputStreamReader = null;
                inputStreamReader = new InputStreamReader(url.openStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                LinkedHashMap<String, Class> classMap = new LinkedHashMap<>();
                //按行读取配置文件
                while ((line = bufferedReader.readLine()) != null) {
                    //如果配置中加入了#开头则表示忽略该类无需进行加载
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] lineArr = line.split("=");
                    String implClassName = lineArr[0];
                    String interfaceName = lineArr[1];
                    classMap.put(implClassName, Class.forName(interfaceName));
                }
                //只会触发class文件的加载，而不会触发对象的实例化
                if (EXTENSION_LOADER_CLASS_CACHE.containsKey(clazz.getName())) {
                    //支持开发者自定义配置
                    EXTENSION_LOADER_CLASS_CACHE.get(clazz.getName()).putAll(classMap);
                } else {
                    EXTENSION_LOADER_CLASS_CACHE.put(clazz.getName(), classMap);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据实现名来实例类，需要类的无参构造方法
     *
     * @param clazz
     * @param implClassName
     * @return
     */
    public <T> T exampleClass(Class<T> clazz, String implClassName) {
        EXTENSION_LOADER.loadExtension(clazz);
        LinkedHashMap<String, Class> classMap = EXTENSION_LOADER_CLASS_CACHE.get(clazz.getName());
        Class aClass = classMap.get(implClassName);
        if (aClass == null) {
            throw new RuntimeException(clazz.getName() + " no match implClass for " + implClassName);
        }
        try {
            Object instance = aClass.newInstance();
            return (T) instance;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
