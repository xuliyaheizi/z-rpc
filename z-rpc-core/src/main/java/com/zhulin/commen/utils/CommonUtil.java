package com.zhulin.commen.utils;

import com.zhulin.commen.channel.ChannelFutureWrapper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/17
 * @Description:
 */
public class CommonUtil {

    public static boolean isEmpty(String str) {
        return str.length() == 0 || str == null;
    }

    public static boolean isEmptyList(List list) {
        if (list == null || list.size() == 0) {
            return true;
        }
        return false;
    }

    public static boolean isNotEmptyList(List list) {
        return !isEmptyList(list);
    }

    /**
     * 获取目标ip地址
     *
     * @return
     */
    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("IP地址获取失败" + e.toString());
        }
        return "";
    }

    public static ChannelFutureWrapper[] convertFromList(List<ChannelFutureWrapper> channelFutureWrappers) {
        ChannelFutureWrapper[] channelFutureWrappersArr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        for (int i = 0; i < channelFutureWrappers.size(); i++) {
            channelFutureWrappersArr[i] = channelFutureWrappers.get(i);
        }
        return channelFutureWrappersArr;
    }

    public static void main(String[] args) {
        System.out.println(getIpAddress());
    }
}
