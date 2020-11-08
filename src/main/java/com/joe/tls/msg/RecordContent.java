package com.joe.tls.msg;

/**
 * record的负载消息
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:46
 */
public interface RecordContent {

    /**
     * 消息序列化为网络传输数据，这里是完整数据，包含header
     * @return 序列化后的网络传输数据
     */
    byte[] serialize();
}
