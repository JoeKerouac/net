package com.joe.tls.msg.impl;

import com.joe.tls.msg.RecordContent;

/**
 * ChangeCipherSpace消息
 * 
 * @author JoeKerouac
 * @data 2020-11-09 11:41
 */
public class ChangeCipherSpace implements RecordContent {

    @Override
    public byte[] serialize() {
        byte[] data = new byte[1];
        data[0] = 1;
        return data;
    }
}
