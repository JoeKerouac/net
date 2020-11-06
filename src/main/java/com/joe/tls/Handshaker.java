package com.joe.tls;

/**
 * @author JoeKerouac
 * @data 2020-11-06 22:34
 */
public abstract class Handshaker {

    protected HandshakeHash    handshakeHash;

    protected SecretCollection secretCollection;

    protected InputRecordStream  inputRecordStream;

    protected OutputRecordStream outputRecordStream;



}
