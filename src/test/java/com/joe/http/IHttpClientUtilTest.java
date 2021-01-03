package com.joe.http;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.request.IHttpRequestBase;
import com.joe.utils.collection.CollectionUtil;
import com.joe.utils.test.WebBaseTest;

/**
 * @author joe
 * @version 2018.04.28 15:48
 */
public class IHttpClientUtilTest extends WebBaseTest {
    private ThreadLocal<IHttpClientUtil> clientHolder = new ThreadLocal<>();
    private ThreadLocal<String> url = new ThreadLocal<>();

    @Test
    public void executeGet() {
        runCase(() -> {
            try {
                String result = clientHolder.get().executeGet(url.get() + "hello");
                Assert.assertEquals("hello", result);
            } catch (Exception e) {
                Assert.assertNull("请求异常", e);
            }
        });
    }

    @Test
    public void executePost() {
        runCase(() -> {
            try {
                String result = clientHolder.get().executePost(url.get() + "helloName", "name=123", "UTF8", "UTF8",
                    IHttpRequestBase.CONTENT_TYPE_FORM);
                Assert.assertEquals("hello : 123", result);
            } catch (Exception e) {
                Assert.assertNull("请求异常", e);
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        url.set(getBaseUrl() + "test/");
        clientHolder.set(new IHttpClientUtil());
    }

    @Override
    protected void destroy() {
        super.destroy();
        url.remove();
        clientHolder.remove();
    }

    @Override
    protected Class<?>[] getSource() {
        return CollectionUtil.addTo(SpringApi.class, super.getSource());
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
