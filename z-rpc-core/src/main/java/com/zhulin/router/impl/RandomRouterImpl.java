package com.zhulin.router.impl;

import com.zhulin.commen.channel.ChannelFutureWrapper;
import com.zhulin.registry.URL;
import com.zhulin.router.Selector;
import com.zhulin.router.ZRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.zhulin.commen.cache.CommonClientCache.*;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/27
 * @Description: 带权重的随机选取策略
 */
public class RandomRouterImpl implements ZRouter {
    @Override
    public void refreshRouterArr(Selector selector) {
        //获取服务提供者的数目
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
        ChannelFutureWrapper[] channelFutureWrapperArr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        //提前生成调用先后顺序的随机数组
        int[] result = createRandomIndex(channelFutureWrapperArr.length);
        //生成对应服务集群的每台机器的调用顺序
        for (int i = 0; i < channelFutureWrappers.size(); i++) {
            channelFutureWrapperArr[i] = channelFutureWrappers.get(i);
        }
        SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(), channelFutureWrapperArr);
        //更新服务节点权重信息
        URL url = new URL();
        url.setServiceName(selector.getProviderServiceName());
        ZROUTER.updateWeight(url);
    }

    /**
     * 创建随机乱序数组
     *
     * @param len
     * @return
     */
    private int[] createRandomIndex(int len) {
        int[] arrInt = new int[len];
        Random ra = new Random();
        for (int i = 0; i < arrInt.length; i++) {
            arrInt[i] = -1;
        }
        int index = 0;
        while (index < arrInt.length) {
            int num = ra.nextInt(len);
            //如果数组中不包含这个元素则赋值给数组
            if (!contains(arrInt, num)) {
                arrInt[index++] = num;
            }
        }
        return arrInt;
    }

    private boolean contains(int[] arr, int key) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == key) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ChannelFutureWrapper select(Selector selector) {
        return CHANNEL_FUTURE_POLLING.getChannelFutureWrapper(selector);
    }

    @Override
    public void updateWeight(URL url) {
        //获取全部的服务提供者
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(url.getServiceName());
        //根据服务的权重信息创建权重数组
        Integer[] weightArr = createWeightArr(channelFutureWrappers);
        Integer[] finalArr = createRandomArr(weightArr);
        ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        for (int i = 0; i < channelFutureWrappers.size(); i++) {
            arr[i] = channelFutureWrappers.get(finalArr[i]);
        }
        SERVICE_ROUTER_MAP.put(url.getServiceName(), arr);
    }

    /**
     * 创建权重数组
     * 约定权重为100的倍数，权重越大，该服务被选中的次数也越多
     *
     * @param channelFutureWrappers
     * @return
     */
    private Integer[] createWeightArr(List<ChannelFutureWrapper> channelFutureWrappers) {
        List<Integer> weightArr = new ArrayList<>();
        for (int i = 0; i < channelFutureWrappers.size(); i++) {
            Integer weight = channelFutureWrappers.get(i).getWeight();
            int c = weight / 100;
            for (int j = 0; j < c; j++) {
                //权重是100的几倍，该服务的下标就被添加几次，也就是该服务占比大一些
                weightArr.add(i);
            }
        }
        Integer[] arr = new Integer[weightArr.size()];
        return weightArr.toArray(arr);
    }

    /**
     * 创建随机乱序数组
     *
     * @param arr
     * @return
     */
    private static Integer[] createRandomArr(Integer[] arr) {
        int total = arr.length;
        Random ra = new Random();
        for (int i = 0; i < total; i++) {
            int j = ra.nextInt(total);
            if (i == j) {
                continue;
            }
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
        return arr;
    }

}
