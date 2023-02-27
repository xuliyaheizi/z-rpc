package com.zhulin.commen.config;

import com.zhulin.commen.constants.RpcConstants;
import com.zhulin.commen.utils.CommonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description:
 */
public class PropertiesLoader {
    private static Properties properties;
    private static final Map<String, String> propertiesMap = new HashMap<>();
    private static final String DEFAULT_PROPERTIES_FILE = RpcConstants.PROPERTIES_FILE;

    /**
     * 读取配置文件数据
     */
    public static void loadConfiguration() {
        if (properties != null) {
            return;
        }
        try (InputStream in =
                     Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("loadServerConfigFromLocal fail,e is {}", e);
        }
    }

    /**
     * 根据键值获取配置属性
     *
     * @param key
     * @return
     */
    public static String getPropertiesStr(String key) {
        if (properties == null) {
            return null;
        }
        if (CommonUtil.isEmpty(key)) {
            return null;
        }
        if (!propertiesMap.containsKey(key)) {
            String value = properties.getProperty(key);
            propertiesMap.put(key, value);
        }
        return propertiesMap.get(key) == null ? null : String.valueOf(propertiesMap.get(key));
    }

    /**
     * 配置参数不能为空
     *
     * @param key
     * @return
     */
    public static String getPropertiesNotBlank(String key) {
        String val = getPropertiesStr(key);
        if (val == null || val.equals("")) {
            throw new IllegalArgumentException(key + " 配置为空异常");
        }
        return val;
    }

    /**
     * 配置参数为空则填入默认值
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static String getPropertiesStrDefault(String key, String defaultVal) {
        String val = getPropertiesStr(key);
        return val == null || val.equals("") ? defaultVal : val;
    }

    /**
     * 根据键值获取配置属性
     *
     * @param key
     * @return
     */
    public static Integer getPropertiesInteger(String key) {
        if (properties == null) {
            return null;
        }
        if (CommonUtil.isEmpty(key)) {
            return null;
        }
        if (!propertiesMap.containsKey(key)) {
            String value = properties.getProperty(key);
            propertiesMap.put(key, value);
        }
        return Integer.valueOf(propertiesMap.get(key));
    }

    /**
     * 根据键值获取配置属性
     *
     * @param key
     * @return
     */
    public static Integer getPropertiesIntegerDefault(String key, Integer defaultVal) {
        if (properties == null) {
            return defaultVal;
        }
        if (CommonUtil.isEmpty(key)) {
            return defaultVal;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            propertiesMap.put(key, String.valueOf(defaultVal));
            return defaultVal;
        }
        if (!propertiesMap.containsKey(key)) {
            propertiesMap.put(key, value);
        }
        return Integer.valueOf(propertiesMap.get(key));
    }
}
