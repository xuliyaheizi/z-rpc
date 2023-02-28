package com.zhulin.client.reference;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author:ZHULIN
 * @Date: 2023/2/24
 * @Description: 代理包装类
 */
@Data
public class RpcReferenceWrapper<T> {
    /**
     * 需要代理实现的类
     */
    private Class<T> aimClass;
    /**
     * 参数扩展集合
     */
    private Map<String, Object> attachments = new HashMap<>();

    public String getUrl() {
        return String.valueOf(attachments.get("url"));
    }

    public void setUrl(String url) {
        attachments.put("url", url);
    }

    public void setTimeOut(int timeOut) {
        attachments.put("timeOut", timeOut);
    }

    public String getTimeOUt() {
        return String.valueOf(attachments.get("timeOut"));
    }

    public String getServiceToken() {
        return String.valueOf(attachments.get("serviceToken"));
    }

    public void setServiceToken(String serviceToken) {
        attachments.put("serviceToken", serviceToken);
    }

    public String getGroup() {
        return String.valueOf(attachments.get("group"));
    }

    public void setGroup(String group) {
        attachments.put("group", group);
    }

    public Boolean isAsync() {
        Object r = attachments.get("async");
        if (r == null || r.equals(false)) {
            return false;
        }
        return Boolean.valueOf(true);
    }

    public void setAsync(boolean async) {
        this.attachments.put("async", async);
    }

    /**
     * 失败重试次数
     */
    public int getRetry() {
        if (attachments.get("retry") == null) {
            return 0;
        } else {
            return (int) attachments.get("retry");
        }
    }

    public void setRetry(int retry) {
        this.attachments.put("retry", retry);
    }
}
