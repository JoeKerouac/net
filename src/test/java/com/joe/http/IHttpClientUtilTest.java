package com.joe.http;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author joe
 * @version 2018.04.28 15:48
 */
public class IHttpClientUtilTest {
    private static final String url = "http://baidu.com";
    private IHttpClientUtil client;

    @Test
    public void init() {
        client = new IHttpClientUtil();
    }

    @Test
    public void executeGet() {
        try {
            String result = client.executeGet(url);
            Assert.assertNotNull("请求失败", result);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }

    @Test
    public void executePost() {
        try {
            String result = client.executePost(url, "");
            Assert.assertNotNull("请求失败", result);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }
}
