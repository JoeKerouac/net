package com.joe.ssl.example;

import com.joe.ssl.openjdk.ssl.SSLServerSocketFactoryImpl;

import java.net.ServerSocket;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-30 18:12
 */
public class SSLServerExample {


    public static void main(String[] args) throws Exception{
        SSLServerSocketFactoryImpl sslServerSocketFactory = new SSLServerSocketFactoryImpl();
        ServerSocket serverSocket = sslServerSocketFactory.createServerSocket();
        serverSocket.accept();
        Thread.sleep(100000);
    }

}
