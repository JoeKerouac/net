package com.joe.http;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author joe
 * @version 2018.04.28 15:48
 */
public class IHttpClientUtilTest {
    private static final String url = "http://baidu.com";

    @Test
    public void executeGet() {
        try {
            IHttpClientUtil clientUtil = new IHttpClientUtil();
            String result = clientUtil.executeGet(url);
            Assert.assertNotNull("请求失败", result);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }

    @Test
    public void executePost() {
        try {
            IHttpClientUtil clientUtil = new IHttpClientUtil();
            String result = clientUtil.executePost(url, "");
            Assert.assertNotNull("请求失败", result);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }
}
