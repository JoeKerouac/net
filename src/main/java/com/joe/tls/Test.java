package com.joe.tls;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;

import javax.net.ssl.X509KeyManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.joe.tls.enums.ContentType;
import com.joe.tls.key.JoeKeyManager;
import com.joe.tls.msg.Record;
import com.joe.tls.msg.impl.ApplicationMsg;

/**
 * @author JoeKerouac
 * @data 2020-11-09 23:56
 */
public class Test {

    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            try {
                testServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "服务端").start();

        Thread.sleep(1000);
        new Thread(() -> {
            try {
                testClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "客户端").start();
    }

    public static void testServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(12345);
        Socket socket = serverSocket.accept();
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(Test.class.getClassLoader().getResourceAsStream("sslserverkeys"), "123456".toCharArray());

        X509KeyManager keyManager = new JoeKeyManager(keyStore, "123456".toCharArray());

        Handshaker handshaker = new Handshaker(inputStream, outputStream, new SecureRandom(), false, keyManager);
        handshaker.kickstart();
        System.out.println("服务端握手完成");
        InputRecordStream inputRecordStream = handshaker.getInputRecordStream();
        OutputRecordStream outputRecordStream = handshaker.getOutputRecordStream();

        ApplicationMsg readMsg = (ApplicationMsg)inputRecordStream.read().msg();
        System.out.println("服务端收到消息：\n-----------\n" + new String(readMsg.getData()));
        System.out.println("\n-----------------\n服务端收到消息打印完毕\n\n\n\n");
        ApplicationMsg writeMsg = new ApplicationMsg("hello world".getBytes());
        outputRecordStream.write(new Record(ContentType.APPLICATION_DATA, TlsVersion.TLS1_2, writeMsg));
    }

    public static void testClient() throws Exception {
        // ip.src == 39.156.66.14 || ip.dst == 39.156.66.14
        Security.addProvider(new BouncyCastleProvider());

        // Socket socket = new Socket("39.156.66.14", 443);
        Socket socket = new Socket("127.0.0.1", 12345);

        String getData = "GET https://www.baidu.com/ HTTP/1.1\r\n" + "Host: www.user.com\r\n"
            + "Connection: Keep-Alive\r\n" + "User-agent: Mozilla/5.0.\r\n\r\n";
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        Handshaker handshaker = new Handshaker(inputStream, outputStream, new SecureRandom(), true);
        handshaker.kickstart();
        System.out.println("握手完成");

        InputRecordStream inputRecordStream = handshaker.getInputRecordStream();
        OutputRecordStream outputRecordStream = handshaker.getOutputRecordStream();
        outputRecordStream
            .write(new Record(ContentType.APPLICATION_DATA, TlsVersion.TLS1_2, new ApplicationMsg(getData.getBytes())));
        Record record = inputRecordStream.read();
        ApplicationMsg msg = (ApplicationMsg)record.msg();
        System.out.println("收到消息：" + new String(msg.getData()));
    }

}
