package com.joe.tls;

import java.io.IOException;

import com.joe.tls.msg.Record;

/**
 * 写出Record
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:32
 */
public interface OutputRecordStream {

    /**
     * 写出一个Record
     * @param record 要写出的Record
     * @throws IOException IO异常
     */
    void write(Record record) throws IOException;

}
