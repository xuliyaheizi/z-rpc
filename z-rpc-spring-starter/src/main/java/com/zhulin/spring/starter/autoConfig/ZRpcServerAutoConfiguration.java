package com.zhulin.spring.starter.autoConfig;

import com.zhulin.commen.cache.CommonServerCache;
import com.zhulin.commen.event.ZRpcListenerLoader;
import com.zhulin.server.RpcServer;
import com.zhulin.server.handler.ServerShutDownHook;
import com.zhulin.server.wrapper.RpcServiceWrapper;
import com.zhulin.spring.starter.common.ZRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/28
 * @Description:
 */
@Slf4j
public class ZRpcServerAutoConfiguration implements InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        RpcServer rpcServer = null;
        //获取有该注解的类
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(ZRpcServer.class);
        if (beanMap.size() == 0) {
            return;
        }
        //输出项目启动信息
        printBanner();
        long beginTime = System.currentTimeMillis();
        rpcServer = new RpcServer();
        rpcServer.initServerConfig();
        ZRpcListenerLoader zRpcListenerLoader = new ZRpcListenerLoader();
        zRpcListenerLoader.init();
        //开始暴露服务
        for (String beanName : beanMap.keySet()) {
            Object bean = beanMap.get(beanName);
            //获取服务的注解信息
            ZRpcServer zRpcServer = bean.getClass().getDeclaredAnnotation(ZRpcServer.class);
            //构建服务注册信息
            RpcServiceWrapper rpcServiceWrapper = new RpcServiceWrapper(bean, zRpcServer.group());
            rpcServiceWrapper.setServiceToken(zRpcServer.serviceToken());
            rpcServiceWrapper.setLimit(zRpcServer.limit());
            rpcServer.registryService(rpcServiceWrapper);
            log.info(">>>>>>>>>>>>> [zrpc] {} registry success >>>>>>>>>>>>>", beanName);
        }
        long endTime = System.currentTimeMillis();
        //启动监听服务注销线程
        ServerShutDownHook.registryShutdownHook();
        //启动netty服务端
        rpcServer.startApplication();
        log.info("============= [{}] started success in {}s =============",
                CommonServerCache.SERVER_CONFIG.getApplicationName(), ((double) endTime - (double) beginTime) / 1000);

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 项目信息输出
     */
    private void printBanner() {
        System.out.println("====================================================================");
        System.out.println("|||------------------ ZRpc Starting Now! ------------------------|||");
        System.out.println("====================================================================");
        System.out.println();
        System.out.println("////////////////////////////////////////////////////////////////////");
        System.out.println("//                          _ooOoo_                               //");
        System.out.println("//                         o8888888o                              //");
        System.out.println("//                         88\" . \"88                              //");
        System.out.println("//                         (| ^_^ |)                              //");
        System.out.println("//                         O\\  =  /O                              //");
        System.out.println("//                      ____/`---'\\____                           //");
        System.out.println("//                    .'  \\\\|     |//  `.                         //");
        System.out.println("//                   /  \\\\|||  :  |||//  \\                        //");
        System.out.println("//                  /  _||||| -:- |||||-  \\                       //");
        System.out.println("//                  |   | \\\\\\  -  /// |   |                       //");
        System.out.println("//                  | \\_|  ''\\---/''  |   |                       //");
        System.out.println("//                  \\  .-\\__  `-`  ___/-. /                       //");
        System.out.println("//                ___`. .'  /--.--\\  `. . ___                     //");
        System.out.println("//             .\"\" '<  `.___\\_<|>_/___.'  >'\" \".                  //");
        System.out.println("//            | | :  `- \\`.;`\\ _ /`;.`/ - ` : | |                 //");
        System.out.println("//            \\  \\ `-.   \\_ __\\ /__ _/   .-` /  /                 //");
        System.out.println("//     ========`-.____`-.___\\_____/___.-`____.-\'========          //");
        System.out.println("//                          `=---=’                               //");
        System.out.println("//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^         //");
        System.out.println("//             佛祖保佑       永不宕机     永无BUG                    //");
        System.out.println("////////////////////////////////////////////////////////////////////");
    }
}
