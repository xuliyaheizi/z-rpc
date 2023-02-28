package com.zhulin.commen.constants;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: z-rpc常量类
 */
public class RpcConstants {
    //自定义协议体魔数
    public static final Short MAGIC_NUM = 2023;
    //默认超时时间
    public static final Integer DEFAULT_TIMEOUT = 5000;
    //zrpc的配置文件
    public static final String PROPERTIES_FILE = "zrpc.properties";
    //默认注册中心
    public static final String DEFAULT_REGISTER_TYPE = "zookeeper";
    //默认序列化方式
    public static final String JDK_SERIALIZE_TYPE = "jdk";
    //默认代理方式
    public static final String JDK_PROXY_TYPE = "jdk";
    //默认负载均衡策略
    public static final String RANDOM_ROUTER_TYPE = "random";
    //自定义协议体分隔符
    public static final String DEFAULT_DECODE_CHAR = "$_i0#Xsop1_$";
    //客户端与服务端最大接收数据包体积
    public static final int SERVER_DEFAULT_MSG_LENGTH = 1024 * 10;
    public static final int CLIENT_DEFAULT_MSG_LENGTH = 1024 * 10;
}
