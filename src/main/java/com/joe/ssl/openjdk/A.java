package com.joe.ssl.openjdk;

/**
 * @author JoeKerouac
 * @version 2020年07月02日 20:25
 */
public class A {
    public static void main(String[] args) {
        System.setProperty("java.version", "123");
        System.out.println(System.getProperty("java.version"));
    }
}
