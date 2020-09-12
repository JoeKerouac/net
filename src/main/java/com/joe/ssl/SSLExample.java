package com.joe.ssl;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-12 16:50
 */
public class SSLExample {

    private static final int    PORT = 12345;

    private static final String IP   = "192.168.15.7";

    public static void main(String[] args) throws Exception {
        new Thread(SSLExample::startServer).start();
        Thread.sleep(1000);
//        new Thread(SSLExample::startClient).start();
    }

    public static void startClient() {
        try {
            //服务器端信息，address和8080；后台连接服务器，还会绑定客户端
            Socket socket = new Socket(IP, PORT);

            try {
                System.out.println("Socket = " + socket);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

                PrintWriter out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                for (int i = 0; i < 10; i++) {
                    out.println("send hello " + i);
                    String str = in.readLine();
                    System.out.println(str);
                }
                out.println("END");
            } finally {
                System.out.println("client closing...");
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void startServer() {
        try {
            ServerSocket server = new ServerSocket(PORT);
            System.out.println("server begin ： " + server);
            try {
                Socket socket = server.accept();
                System.out.println("Connection socket: " + socket);
                try {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                    //Output is automatically flushed by PrintWrite
                    PrintWriter out = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    while (true) {
                        String str = in.readLine();
                        if ("END".equals(str))
                            break;
                        System.out.println("received : " + str);
                        out.println(str);
                    }
                } finally {
                    System.out.println("Server closing....");
                    socket.close();
                }
            } finally {
                server.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
