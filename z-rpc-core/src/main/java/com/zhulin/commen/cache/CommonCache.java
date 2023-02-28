package com.zhulin.commen.cache;

import com.zhulin.spi.ExtensionLoader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 全局缓存
 */
public class CommonCache {
    /**
     * SPI机制加载器
     */
    public static ExtensionLoader EXTENSION_LOADER = new ExtensionLoader();
    /**
     * 存储从配置文件获取的SPI类
     */
    public static Map<String, LinkedHashMap<String, Class>> EXTENSION_LOADER_CLASS_CACHE = new ConcurrentHashMap<>();
}
