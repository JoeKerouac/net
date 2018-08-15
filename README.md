# net
网络请求库，方便开发者快速便捷的发起网络请求

# 使用说明：
本SDK提供两种请求方法：
- IHttpClientUtil,通过IHttpClientUtil可以快速的构建HTTP请求，可以满足大多数场景；
- IHttpClient，通过IHttpClient可以定制许多HTTP请求的细节，满足精细化场景；

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