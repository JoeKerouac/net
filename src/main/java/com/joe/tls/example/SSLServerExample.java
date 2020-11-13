package com.joe.tls.example;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.joe.ssl.openjdk.ssl.SSLContextImpl;
import com.joe.ssl.openjdk.ssl.SSLServerSocketFactoryImpl;
import com.joe.ssl.openjdk.ssl.SSLSocketFactoryImpl;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-30 18:12
 */
public class SSLServerExample {

    public static void main(String[] args) throws Exception {
        //        client();
        server();
    }

    public static void client() throws Exception {
        SSLContextImpl sslContext = new SSLContextImpl.DefaultSSLContext() {
            @Override
            public X509TrustManager chooseTrustManager(TrustManager[] tm) throws KeyManagementException {
                return super.chooseTrustManager(tm);
            }
        };

        SSLSocketFactoryImpl socketFactory = new SSLSocketFactoryImpl();
        Socket socket = socketFactory.createSocket("39.156.66.14", 443);
        OutputStream out = socket.getOutputStream();
        out.write("GET".getBytes());
        out.flush();
        System.out.println(socket);
    }

    public static void server() throws Exception {
        // 运行需要的参数
        //        -Djava.security.debug=all -Djavax.net.debug=all -Djavax.net.ssl.keyStore=D:\temp\ssl\sslserverkeys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=D:\temp\ssl\sslservertrust -Djavax.net.ssl.trustStorePassword=123456
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
        ServerSocket serverSocket = serverSocketFactory.createServerSocket(12345);

        Socket socket = serverSocket.accept();
        socket.getOutputStream().write("hello world".getBytes());
        socket.getOutputStream().flush();

        System.out.println("收到链接了");
        Thread.sleep(1000 * 30000);
    }

}
