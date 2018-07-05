package com.joe.http.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

/**
 * @author joe
 * @version 2018.07.05 17:35
 */
@Slf4j
public class SSLTools {

    /**
     * 加载指定证书对应的SSLContext
     *
     * @param path     证书本地位置
     * @param type     证书类型，例如PKCS12、JKS等
     * @param password 证书密码，没有可以不填
     * @return 指定证书对应的SSLContext，证书加载失败返回null
     */
    public static SSLContext build(String path, String type, String password) {
        try {
            return build(new FileInputStream(path), type, password);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 加载指定证书对应的SSLContext
     *
     * @param inputStream 证书的输入流（方法内部会关闭该流）
     * @param type        证书类型，例如PKCS12、JKS等
     * @param password    证书密码，没有可以不填
     * @return 指定证书对应的SSLContext，证书加载失败返回null
     */
    public static SSLContext build(InputStream inputStream, String type, String password) {
        try (InputStream stream = inputStream) {
            KeyStore keyStore = KeyStore.getInstance(type);
            char[] passwordChars = password == null ? null : password.toCharArray();
            keyStore.load(stream, passwordChars);
            return SSLContexts.custom()
                    .loadKeyMaterial(keyStore, passwordChars)
                    .build();
        } catch (Throwable e) {
            log.error("SSLContext加载失败", e);
            return null;
        }
    }
}
