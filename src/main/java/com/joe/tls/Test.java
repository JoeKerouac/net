package com.joe.tls;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.joe.tls.enums.ContentType;
import com.joe.tls.msg.Record;
import com.joe.tls.msg.impl.ApplicationMsg;

/**
 * @author JoeKerouac
 * @data 2020-11-09 23:56
 */
public class Test {

    public static void main(String[] args) throws Exception {
        test();
    }

    public static void test() throws Exception {
        // ip.src == 39.156.66.14 || ip.dst == 39.156.66.14
        Security.addProvider(new BouncyCastleProvider());

        Socket socket = new Socket("39.156.66.14", 443);
        //        Socket socket = new Socket("127.0.0.1", 12345);

        String getData = "GET https://www.baidu.com/ HTTP/1.1\r\n" + "Host: www.user.com\r\n"
                         + "Connection: Keep-Alive\r\n" + "User-agent: Mozilla/5.0.\r\n\r\n";
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        Handshaker handshaker = new Handshaker(inputStream, outputStream, new SecureRandom(), true);
        handshaker.kickstart();
        System.out.println("握手完成");

        OutputRecordStream outputRecordStream = handshaker.outputRecordStream;
        InputRecordStream inputRecordStream = handshaker.inputRecordStream;
        outputRecordStream.write(new Record(ContentType.APPLICATION_DATA, TlsVersion.TLS1_2,
            new ApplicationMsg(getData.getBytes())));
        Record record = inputRecordStream.read();
        ApplicationMsg msg = (ApplicationMsg) record.msg();
        System.out.println("收到消息：" + new String(msg.getData()));
    }

}
