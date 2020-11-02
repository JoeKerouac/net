package com.joe.ssl.message;

import java.io.IOException;

/**
 * 将消息写入输出流
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-19 16:00
 */
public interface RecordMessage {

    /**
     * 将消息写入输出流
     *
     * @param stream 输出流
     * @throws IOException IO异常
     */
    void write(WrapedOutputStream stream) throws IOException;
}
