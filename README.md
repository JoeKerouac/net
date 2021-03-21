# net
网络请求库，方便开发者快速便捷的发起网络请求

# 使用说明：
本SDK提供三种请求方法：
- IHttpClient:通过IHttpClient可以定制许多HTTP请求的细节，满足精细化场景；
- IHttpClientUtil:通过IHttpClientUtil可以快速的构建HTTP请求，可以满足大多数场景；
- ResourceFactory:通过ResourceFactory可以快速调用springMVC/jersey编写的接口，可以用来快速测试。

# 使用示例：
## IHttpClient

```java
package com.joe.http;

import java.nio.charset.Charset;

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

/**
 * @author JoeKerouac
 * @version 2018.04.28 15:48
 */
public class IHttpClientTest extends WebBaseTest {
    private ThreadLocal<IHttpClient> clientHolder = new ThreadLocal<>();
    private ThreadLocal<String> url = new ThreadLocal<>();

    @Test
    public void executeGet() {
        runCase(() -> {
            IHttpGet get = IHttpGet.builder(url.get() + "hello").charset("utf8").build();
            try {
                doRequest(clientHolder.get(), get, "hello");
            } catch (Exception e) {
                Assert.assertNull("请求异常", e);
            }
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
```
可以看出IHttpClient可以定制化很多信息，例如连接超时时间、请求超时时间等，上述示例的配置并不全，详细的配置信息可以看API（还可以定制SSLContext、CookieStore等，同时对于每个请求信息也能单独定制），但是请求相对复杂；

## IHttpClientUtil

```java
package com.joe.http;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.request.IHttpRequestBase;

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
                String result = clientHolder.get().executePost(url.get() + "helloName", "name=123",
                        "UTF8", "UTF8", IHttpRequestBase.CONTENT_TYPE_FORM);
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
```
可以看出使用IHttpClientUtil的方式发起请求很方便，但是不能对每个请求做定制，不过也能满足大多数场景了，同时还可以自定义一个IHttpClient然后通过构造参数传给IHttpClientUtil，这样就能使用现有的已经定制的IHttpClient了。

## ResourceFactory

```java
package com.joe.http.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.request.IHttpRequestBase;
import com.joe.http.ws.core.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author joe
 * @version 2018.08.23 14:28
 */
@SpringBootApplication
public class ResourceFactoryTest extends WebBaseTest {

    @Test
    public void doSpringResourceAnalyzeTest() {
        runCase(() -> {
            ResourceFactory factory = new ResourceFactory(getBaseUrl(), ResourceType.SPRING);
            Resource resource = factory.build(SpringApi.class);
            doTest(resource);
        });
    }

    @Test
    public void doJerseyResourceAnalyzeTest() {
        runCase(() -> {
            ResourceFactory factory = new ResourceFactory(getBaseUrl(), ResourceType.JERSEY);
            Resource resource = factory.build(JerseyResource.class);
            doTest(resource);
        });
    }

    @Test
    public void doSpringResourceAnalyzeListTest() {
        runCase(() -> {
            ResourceFactory factory = new ResourceFactory(getBaseUrl(), ResourceType.SPRING);
            Resource resource = factory.build(SpringApi.class);
            ArrayList<String> data = new ArrayList<>();
            data.add("123");
            data.add("121231233");
            data.add("123123");
            data.add("12sadljfklasdjflkjaswd3");
            data.add("12sadljfklas-09-k'ld3");
            Assert.assertEquals("结果与预期不符", data.size(), resource.size(data));
        });
    }

    @Test
    public void doSpringResourceAnalyzeMapTest() {
        runCase(() -> {
            ResourceFactory factory = new ResourceFactory(getBaseUrl(), ResourceType.SPRING);
            Resource resource = factory.build(SpringApi.class);
            Map<String, Object> map = new HashMap<>();
            map.put("123", 123);
            map.put("232", 2323);
            map.put("1233", "123123");
            Assert.assertEquals("结果与预期不符", map.size(), resource.size(map));
        });
    }

    @Test
    public void doSpringResourceAnalyzeUserTest() {
        runCase(() -> {
            ResourceFactory factory = new ResourceFactory(getBaseUrl(), ResourceType.SPRING);
            Resource resource = factory.build(SpringApi.class);
            User user = new User("JoeKerouac", 23, "男");
            Assert.assertEquals("结果与预期不符", user, resource.user(user));
        });
    }

    @Test
    public void doSpringResourceAnalyzeJsonUserTest() {
        runCase(() -> {
            ResourceFactory factory = new ResourceFactory(getBaseUrl(), ResourceType.SPRING);
            Resource resource = factory.build(SpringApi.class);
            User user = new User("JoeKerouac", 23, "man");
            Assert.assertEquals("结果与预期不符", user,
                    resource.formUser(user.getName(), user.getAge(), user.getSex()));
        });
    }

    private void doTest(Resource resource) {
        String name = "joe";
        String result = resource.hello(name);
        Assert.assertEquals(result, name);
    }

    @Controller
    @RequestMapping("test")
    public static class SpringApi implements Resource {
        @RequestMapping(value = "hello")
        @ResponseBody
        @Override
        public String hello(String name) {
            return name;
        }

        @RequestMapping(value = "list")
        @ResponseBody
        @Override
        public int size(@RequestBody List<String> data) {
            return data.size();
        }

        @RequestMapping(value = "map")
        @ResponseBody
        @Override
        public int size(@RequestBody Map<String, Object> data) {
            return data.size();
        }

        @RequestMapping(value = "user")
        @ResponseBody
        @Override
        public User user(@RequestBody User user) {
            return user;
        }

        @RequestMapping(value = "jsonUser", consumes = IHttpRequestBase.CONTENT_TYPE_FORM)
        @ResponseBody
        @Override
        public User formUser(String name, Integer age, @RequestHeader("sex") String sex) {
            return new User(name, age, sex);
        }
    }

    public interface Resource {
        /**
         * 简单类型测试
         * @param name 传入参数
         * @return 传入参数原路返回
         */
        String hello(String name);

        /**
         * 测试List
         * @param data 传入参数
         * @return 传入List的长度
         */
        int size(List<String> data);

        /**
         * 测试Map
         * @param data 传入参数
         * @return 传入Map的长度
         */
        int size(Map<String, Object> data);

        /**
         * 测试复杂类型
         * @param user 传入user
         * @return 传入user原样返回
         */
        User user(User user);

        /**
         * 传入User的json数据返回User对象
         * @param name 名字
         * @param age 年龄
         * @param sex 性别
         * @return 构建的user对象
         */
        User formUser(String name, Integer age, String sex);
    }

    @Path("test")
    public interface JerseyResource extends Resource {
        @POST
        @Path("hello")
        String hello(@FormParam("name") String name);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private String name;
        private int age;
        private String sex;
    }
}
```

可以看出，使用ResourceFactory可以像调用本地方法一样调用springMVC编写的接口，前提是需要依赖接口类，例如上述示例中依赖了SpringApi，在实际中基本都是代码编写好后部署到服务器运行，然后在本地调用测试，这时只需要构建ResourceFactory时将远程base-url传入即可，然后将Controller作为依赖引入或者自己编写签名相同的Controller调用即可。调用jersey只需要将ResourceFactory的构造参数SPRING更改为JERSEY即可，后续调用基本一致（接口声明使用的注解不一样）。