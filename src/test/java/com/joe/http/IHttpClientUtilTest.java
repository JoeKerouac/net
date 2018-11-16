package com.joe.http;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.request.IHttpRequestBase;
import com.joe.utils.test.WebTestBase;

/**
 * @author joe
 * @version 2018.04.28 15:48
 */
public class IHttpClientUtilTest extends WebTestBase {
    private static final IHttpClientUtil CLIENT = new IHttpClientUtil();
    private ThreadLocal<String>          url    = new ThreadLocal<>();

    @Test
    public void executeGet() {
        run(() -> {
            try {
                String result = CLIENT.executeGet(url.get() + "hello");
                Assert.assertEquals("hello", result);
            } catch (Exception e) {
                Assert.assertNull("请求异常", e);
            }
        });
    }

    @Test
    public void executePost() {
        run(() -> {
            try {
                String result = CLIENT.executePost(url.get() + "helloName", "name=123", "UTF8",
                    "UTF8", IHttpRequestBase.CONTENT_TYPE_FORM);
                Assert.assertEquals("hello : 123", result);
            } catch (Exception e) {
                Assert.assertNull("请求异常", e);
            }
        });
    }

    @Override
    protected void init() {
        url.set(getBaseUrl() + "test/");
    }

    @Override
    protected void destroy() {
        url.remove();
    }

    @Controller
    @RequestMapping("test")
    public static class SpringApi {
        @RequestMapping(value = "helloName")
        @ResponseBody
        public String helloName(String name) {
            return "hello : " + name;
        }

        @RequestMapping(value = "hello")
        @ResponseBody
        public String hello() {
            return "hello";
        }
    }
}
