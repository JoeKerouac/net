package com.joe.tls;

import java.io.IOException;

import com.joe.tls.msg.Record;

/**
 * 读取RecordMessage
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:32
 */
public interface InputRecordStream {

    /**
     * 读取一个RecordMessage
     * @return RecordMessage
     * @throws IOException IO异常
     */
    Record read() throws IOException;

}
