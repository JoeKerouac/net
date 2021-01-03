package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import com.joe.tls.msg.RecordContent;

import lombok.Getter;

/**
 * 应用层消息
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-12 14:22
 */
public class ApplicationMsg implements RecordContent {

    /**
     * 应用层数据
     */
    @Getter
    private final byte[] data;

    public ApplicationMsg(byte[] data) {
        this.data = data;
    }

    public ApplicationMsg(ByteBuffer buffer) {
        if (!buffer.isDirect()) {
            this.data = buffer.array();
        } else {
            this.data = new byte[buffer.limit() - buffer.position()];
            buffer.get(data);
        }
    }

    @Override
    public byte[] serialize() {
        return data;
    }
}
