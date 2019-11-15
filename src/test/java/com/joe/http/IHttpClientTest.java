package com.joe.http;

import java.nio.charset.Charset;

import com.joe.utils.collection.CollectionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.client.IHttpClient;
import com.joe.http.config.IHttpClientConfig;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;
import com.joe.utils.test.WebBaseTest;

/**
 * @author JoeKerouac
 * @version 2018.04.28 15:48
 */
public class IHttpClientTest extends WebBaseTest {
    private ThreadLocal<IHttpClient> clientHolder = new ThreadLocal<>();
    private ThreadLocal<String>      url          = new ThreadLocal<>();

    @Test
    public void executeGet() {
        runCase(() -> {
            Exception expect = null;
            IHttpGet get = IHttpGet.builder(url.get() + "hello").charset("utf8").build();
            try {
                doRequest(clientHolder.get(), get, "hello");
            } catch (Exception e) {
                expect = e;
            }
            Assert.assertNull("请求异常", expect);
        });
    }

    @Test
    public void executePost() {
        runCase(() -> {
            IHttpPost post = IHttpPost.builder(url.get() + "helloName")
                .contentType(IHttpRequestBase.CONTENT_TYPE_FORM).formParam("name", "123")
                .charset("utf8").build();
            try {
                doRequest(clientHolder.get(), post, "hello : 123");
            } catch (Exception e) {
                Assert.assertNull("请求异常", e);
            }
        });
    }

    private void doRequest(IHttpClient client, IHttpRequestBase request,
                           String result) throws Exception {
        IHttpResponse response = client.execute(request);
        String realResult = response.getResult();
        int status = response.getStatus();
        Assert.assertEquals("请求异常，请求状态码错误", 200, status);
        Assert.assertEquals("请求异常，预期结果与实际不符", result, realResult);
    }

    @Override
    public void init() {
        super.init();
        IHttpClientConfig config = new IHttpClientConfig();
        config.setCharset(Charset.defaultCharset());
        config.setRcvBufSize(1024);
        config.setSndBufSize(1024);
        config.setConnectionRequestTimeout(1000 * 5);
        config.setSocketTimeout(1000 * 60);
        config.setConnectTimeout(1000 * 30);
        clientHolder.set(IHttpClient.builder().config(config).build());

        url.set(getBaseUrl() + "test/");
    }

    @Override
    public void destroy() {
        super.destroy();
        clientHolder.remove();
        url.remove();
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
