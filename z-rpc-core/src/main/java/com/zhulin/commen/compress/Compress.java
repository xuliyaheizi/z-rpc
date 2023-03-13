package com.zhulin.commen.compress;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/5
 * @Description: 压缩
 */
public interface Compress {

    /**
     * 压缩
     *
     * @param bytes
     * @return
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压
     *
     * @param bytes
     * @return
     */
    byte[] desCompress(byte[] bytes);
}
