package com.joe.ssl.example;

import com.joe.ssl.openjdk.ssl.SSLServerSocketFactoryImpl;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-30 18:12
 */
public class SSLServerExample {


    public static void main(String[] args) throws Exception{
//        SSLServerSocketFactoryImpl sslServerSocketFactory = new SSLServerSocketFactoryImpl();
//        ServerSocket serverSocket = sslServerSocketFactory.createServerSocket();
//        serverSocket.accept();
//        Thread.sleep(100000);

        ServerSocketFactory serverSocketFactory = SSLServerSocketFactory.getDefault();
        ServerSocket serverSocket = serverSocketFactory.createServerSocket(12345);
        Socket socket = serverSocket.accept();
        System.out.println("收到链接了");
        Thread.sleep(1000 * 30000);
    }

}
