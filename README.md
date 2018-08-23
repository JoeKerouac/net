# net
网络请求库，方便开发者快速便捷的发起网络请求

# 使用说明：
本SDK提供三种请求方法：
- [IHttpClient](#IHttpClient):通过IHttpClient可以定制许多HTTP请求的细节，满足精细化场景；
- [IHttpClientUtil](#IHttpClientUtil):通过IHttpClientUtil可以快速的构建HTTP请求，可以满足大多数场景；
- [ResourceFactory](#ResourceFactory):通过ResourceFactory可以快速调用springMVC/jersey编写的接口，可以用来快速测试。

# 使用示例：
## IHttpClient
```java
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.joe.http.client.IHttpClient;
import com.joe.http.config.IHttpClientConfig;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;

/**
 * @author joe
 * @version 2018.04.28 15:48
 */
public class IHttpClientTest {
    private static final String url = "http://baidu.com";
    private IHttpClient         client;

    @Before
    public void init() {
        IHttpClientConfig config = new IHttpClientConfig();
        config.setCharset(Charset.defaultCharset());
        config.setRcvBufSize(1024);
        config.setSndBufSize(1024);
        config.setConnectionRequestTimeout(1000 * 5);
        config.setSocketTimeout(1000 * 60);
        config.setConnectTimeout(1000 * 30);
        client = IHttpClient.builder().config(config).build();
    }

    @After
    public void destroy() throws Exception {
        client.close();
    }

    @Test
    public void executeGet() {
        IHttpGet get = IHttpGet.builder().url(url).charset("utf8").build();
        try {
            int status = client.execute(get).getStatus();
            Assert.assertEquals("请求异常", 200, status);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }

    @Test
    public void executePost() {
        IHttpPost post = IHttpPost.builder().url(url).charset("utf8").build();
        try {
            int status = client.execute(post).getStatus();
            Assert.assertEquals("请求异常", 302, status);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }

}
```
可以看出IHttpClient可以定制化很多信息，例如连接超时时间、请求超时时间等，上述示例的配置并不全，详细的配置信息可以看API（还可以定时SSLContext、CookieStore等，同时对于每个请求信息也能单独定制），但是请求相对复杂；

## IHttpClientUtil
```java
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

```
可以看出使用IHttpClientUtil的方式发起请求很方便，但是不能对每个请求做定制，不过也能满足大多数场景了，同时还可以自定义一个IHttpClient然后通过构造参数传给IHttpClientUtil，这样就能使用现有的已经定制的IHttpClient了。

## ResourceFactory
```java
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author joe
 * @version 2018.08.23 14:28
 */
@SpringBootApplication
public class ResourceFactoryTest {
    ConfigurableApplicationContext context;

    @Before
    public void init() {
        context = SpringApplication.run(ResourceFactoryTest.class, new String[0]);
    }

    @After
    public void destroy() {
        context.close();
    }

    @Test
    public void doSpringResourceAnalyzeTest() {
        ResourceFactory factory = new ResourceFactory("http://127.0.0.1", ResourceType.SPRING);
        SpringApi api = factory.build(SpringApi.class);
        String name = "joe";
        String result = api.hello(name);
        Assert.assertEquals(result, "hello : " + name);
    }

    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.setPort(80);
        return factory;
    }

    @Controller
    @RequestMapping("spring")
    static class SpringApi {
        @RequestMapping(value = "hello")
        @ResponseBody
        public String hello(String name) {
            return "hello : " + name;
        }
    }
}
```

可以看出，使用ResourceFactory可以像调用本地方法一样调用springMVC编写的接口，前提是需要依赖接口类，例如上述示例中依赖了SpringApi，在实际中基本都是代码编写好后部署到服务器运行，然后在本地调用测试，这时只需要构建ResourceFactory时将远程base-url传入即可，然后将Controller作为依赖引入或者自己编写签名相同的Controller调用即可。调用jersey只需要将ResourceFactory的构造参数SPRING更改为JERSEY即可，后续调用基本一致（接口声明使用的注解不一样）。