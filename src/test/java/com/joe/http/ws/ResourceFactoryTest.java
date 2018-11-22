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
import com.joe.utils.test.WebBaseTest;

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
        private int    age;
        private String sex;
    }
}
