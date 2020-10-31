package com.joe.ssl.example;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import com.joe.ssl.openjdk.ssl.SSLServerSocketFactoryImpl;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-30 18:12
 */
public class SSLServerExample {

    public static void main(String[] args) throws Exception {
        // 运行需要的参数
        //        -Djava.security.debug=all -Djavax.net.debug=all -Djavax.net.ssl.keyStore=E:\temp\ssl\sslserverkeys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=E:\temp\ssl\sslservertrust -Djavax.net.ssl.trustStorePassword=123456
        // 对应的证书生成，注意，证书算法必须指定RSA，不然默认是DSA
        //        keytool -genkey -alias sslclient -keyalg RSA -keystore sslclientkeys
        //        keytool -export -alias sslclient -keystore sslclientkeys -file sslclient.cer
        //
        //        keytool -genkey -alias sslserver -keyalg RSA -keystore sslserverkeys
        //        keytool -export -alias sslserver -keystore sslserverkeys -file sslserver.cer
        //
        //        keytool -import -alias sslclient -keystore sslservertrust -file sslclient.cer
        //        keytool -import -alias sslserver -keystore sslclienttrust -file sslserver.cer

        //        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();

        SSLServerSocketFactoryImpl serverSocketFactory = new SSLServerSocketFactoryImpl();
        System.out.println(Arrays.toString(serverSocketFactory.getDefaultCipherSuites()));
        ServerSocket serverSocket = serverSocketFactory.createServerSocket(12345);

        Socket socket = serverSocket.accept();
        socket.getOutputStream().write("hello world".getBytes());
        socket.getOutputStream().flush();

        System.out.println("收到链接了");
        Thread.sleep(1000 * 30000);
    }

}
