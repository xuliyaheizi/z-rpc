package com.zhulin.spring.starter.autoConfig;

import com.zhulin.client.RpcClient;
import com.zhulin.client.handler.ConnectionHandler;
import com.zhulin.client.reference.RpcReference;
import com.zhulin.client.reference.RpcReferenceWrapper;
import com.zhulin.commen.cache.CommonClientCache;
import com.zhulin.spring.starter.common.ZRpcReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description:
 */
@Slf4j
public class ZRpcClientAutoConfiguration implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent> {
    private static RpcReference rpcReference;
    private static RpcClient rpcClient = null;
    private volatile boolean needInitClient = false;
    private volatile boolean hasInitClientConfig = false;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //获取bean的属性
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            //判断属性是否有ZRpcReference注解
            if (field.isAnnotationPresent(ZRpcReference.class)) {
                //判断客户端配置文件是否初始化
                if (!hasInitClientConfig) {
                    rpcClient = new RpcClient();
                    try {
                        //获取代理对象
                        rpcReference = rpcClient.initApplication();
                    } catch (Exception e) {
                        log.error("[IRpcClientAutoConfiguration] postProcessAfterInitialization has error ", e);
                        throw new RuntimeException(e);
                    }
                    hasInitClientConfig = true;
                }
                needInitClient = true;
                //获取注解信息
                ZRpcReference zRpcReference = field.getDeclaredAnnotation(ZRpcReference.class);
                try {
                    //通过setAccessible(true)的方式关闭安全检查就可以达到提升反射速度的目的
                    field.setAccessible(true);
                    //获取属性的值
                    Object refObj = field.get(bean);
                    //构建调用服务的请求信息
                    RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
                    //field.getType()获取属性的类型
                    rpcReferenceWrapper.setAimClass(field.getType());
                    rpcReferenceWrapper.setGroup(zRpcReference.group());
                    rpcReferenceWrapper.setServiceToken(zRpcReference.serviceToken());
                    rpcReferenceWrapper.setUrl(zRpcReference.url());
                    rpcReferenceWrapper.setTimeOut(zRpcReference.timeOut());
                    rpcReferenceWrapper.setRetry(zRpcReference.retry());
                    rpcReferenceWrapper.setAsync(zRpcReference.async());
                    //订阅服务
                    rpcClient.doSubscribeService(field.getType());
                    //通过代理获取代理对象
                    refObj = rpcReference.get(rpcReferenceWrapper);
                    field.set(bean, refObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (needInitClient && rpcClient != null) {
            log.info("============== [{}] started success ==============",
                    CommonClientCache.CLIENT_CONFIG.getApplicationName());
            ConnectionHandler.bootstrap = rpcClient.getBootstrap();
            rpcClient.doConnectServer();
            rpcClient.startSendMsg();
        }
    }
}
