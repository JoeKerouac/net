package com.joe.tls;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author JoeKerouac
 * @data 2020-11-09 23:56
 */
public class Test {

    public static void main(String[] args) throws Exception {
        // ip.src == 39.156.66.14 || ip.dst == 39.156.66.14
        //        Socket socket = new Socket("192.168.1.111", 12345);
        Security.addProvider(new BouncyCastleProvider());

//        Socket socket = new Socket("39.156.66.14", 443);
                Socket socket = new Socket("127.0.0.1", 12345);

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        ClientHandshaker handshaker = new ClientHandshaker(inputStream, outputStream,
            new SecureRandom());
        handshaker.kickstart();
    }

}
