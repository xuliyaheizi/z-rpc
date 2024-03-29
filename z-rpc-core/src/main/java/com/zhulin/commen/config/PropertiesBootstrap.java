package com.zhulin.commen.config;

import static com.zhulin.commen.constants.RpcConstants.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description:
 */
public class PropertiesBootstrap {
    private volatile boolean configIsReady;
    public static final String SERVER_PORT = "zrpc.serverPort";
    public static final String REGISTER_ADDRESS = "zrpc.registerAddr";
    public static final String REGISTER_TYPE = "zrpc.registerType";
    public static final String APPLICATION_NAME = "zrpc.applicationName";
    public static final String PROXY_TYPE = "zrpc.proxyType";
    public static final String ROUTER_TYPE = "zrpc.router";
    public static final String SERVER_SERIALIZE_TYPE = "zrpc.serverSerialize";
    public static final String CLIENT_SERIALIZE_TYPE = "zrpc.clientSerialize";
    public static final String CLIENT_DEFAULT_TIME_OUT = "zrpc.client.default.timeout";
    public static final String SERVER_BIZ_THREAD_NUMS = "zrpc.server.biz.thread.nums";
    public static final String SERVER_QUEUE_SIZE = "zrpc.server.queue.size";
    public static final String SERVER_MAX_CONNECTION = "zrpc.server.max.connection";
    public static final String SERVER_MAX_DATA_SIZE = "zrpc.server.max.data.size";
    public static final String CLIENT_MAX_DATA_SIZE = "zrpc.client.max.data.size";

    /**
     * 加载服务端配置文件
     *
     * @return
     */
    public static ServerConfig loadServiceConfigFormLocal() {
        //加载配置文件
        PropertiesLoader.loadConfiguration();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setServerPort(PropertiesLoader.getPropertiesInteger(SERVER_PORT));
        serverConfig.setApplicationName(PropertiesLoader.getPropertiesStr(APPLICATION_NAME));
        serverConfig.setRegisterAddr(PropertiesLoader.getPropertiesStr(REGISTER_ADDRESS));
        serverConfig.setRegisterType(PropertiesLoader.getPropertiesStrDefault(REGISTER_TYPE, DEFAULT_REGISTER_TYPE));
        serverConfig.setServerSerialize(PropertiesLoader.getPropertiesStrDefault(SERVER_SERIALIZE_TYPE,
                JDK_SERIALIZE_TYPE));
        serverConfig.setServerBizThreadNums(PropertiesLoader.getPropertiesIntegerDefault(SERVER_BIZ_THREAD_NUMS,
                DEFAULT_THREAD_NUMS));
        serverConfig.setServerQueueSize(PropertiesLoader.getPropertiesIntegerDefault(SERVER_QUEUE_SIZE,
                DEFAULT_QUEUE_SIZE));
        serverConfig.setMaxConnections(PropertiesLoader.getPropertiesIntegerDefault(SERVER_MAX_CONNECTION,
                DEFAULT_MAX_CONNECTION_NUMS));
        serverConfig.setMaxServerRequestData(PropertiesLoader.getPropertiesIntegerDefault(SERVER_MAX_DATA_SIZE,
                SERVER_DEFAULT_MSG_LENGTH));
        return serverConfig;
    }

    /**
     * 加载客户端配置文件
     *
     * @return
     */
    public static ClientConfig loadClientConfigFromLocal() {
        PropertiesLoader.loadConfiguration();
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setApplicationName(PropertiesLoader.getPropertiesNotBlank(APPLICATION_NAME));
        clientConfig.setRegisterAddr(PropertiesLoader.getPropertiesNotBlank(REGISTER_ADDRESS));
        clientConfig.setRegisterType(PropertiesLoader.getPropertiesStrDefault(REGISTER_TYPE, DEFAULT_REGISTER_TYPE));
        clientConfig.setProxyType(PropertiesLoader.getPropertiesStrDefault(PROXY_TYPE, JDK_PROXY_TYPE));
        clientConfig.setRouterStrategy(PropertiesLoader.getPropertiesStrDefault(ROUTER_TYPE, RANDOM_ROUTER_TYPE));
        clientConfig.setClientSerialize(PropertiesLoader.getPropertiesStrDefault(CLIENT_SERIALIZE_TYPE,
                JDK_SERIALIZE_TYPE));
        clientConfig.setTimeOut(PropertiesLoader.getPropertiesIntegerDefault(CLIENT_DEFAULT_TIME_OUT,
         DEFAULT_TIMEOUT));
        clientConfig.setMaxServerRespDataSize(PropertiesLoader.getPropertiesIntegerDefault(CLIENT_MAX_DATA_SIZE,
                CLIENT_DEFAULT_MSG_LENGTH));
        return clientConfig;
    }
}
