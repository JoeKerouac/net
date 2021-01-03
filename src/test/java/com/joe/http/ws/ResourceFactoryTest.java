package com.joe.http.ws;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.exception.ServerException;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.ws.core.ResourceType;
import com.joe.utils.collection.CollectionUtil;
import com.joe.utils.test.WebBaseTest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author joe
 * @version 2018.08.23 14:28
 */
public class ResourceFactoryTest extends WebBaseTest {

    private static final ThreadLocal<Resource> SPRING_RESOURCE_FACTORY_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Resource> JERSEY_RESOURCE_FACTORY_THREAD_LOCAL = new ThreadLocal<>();

    @Test
    public void doTestJerseyResourceAnalyze() {
        runCase(() -> doTestHello(JERSEY_RESOURCE_FACTORY_THREAD_LOCAL.get()));
    }

    @Test
    public void doTestSpringResourceAnalyzeHello() {
        runCase(() -> doTestHello(SPRING_RESOURCE_FACTORY_THREAD_LOCAL.get()));
    }

    @Test
    public void doTestSpringResourceAnalyzeSize() {
        runCase(() -> doTestSize(SPRING_RESOURCE_FACTORY_THREAD_LOCAL.get()));
    }

    @Test
    public void doTestSpringResourceAnalyzeUser() {
        runCase(() -> doTestUser(SPRING_RESOURCE_FACTORY_THREAD_LOCAL.get()));
    }

    @Test
    public void doTestSpringResourceAnalyzeJsonUser() {
        runCase(() -> doTestFormUser(SPRING_RESOURCE_FACTORY_THREAD_LOCAL.get()));
    }

    @Test
    public void doTestSpringResourceAnalyzeException() {
        runCase(() -> doTestException(SPRING_RESOURCE_FACTORY_THREAD_LOCAL.get()));
    }

    @Override
    protected void init() {
        super.init();
        ResourceFactory jerseyResourceFactory = new ResourceFactory(getBaseUrl(), ResourceType.JERSEY);
        Resource jerseyResource = jerseyResourceFactory.build(JerseyResource.class, StandardCharsets.UTF_8.name());
        JERSEY_RESOURCE_FACTORY_THREAD_LOCAL.set(jerseyResource);

        ResourceFactory springResourceFactory = new ResourceFactory(getBaseUrl(), ResourceType.SPRING);
        Resource springResource = springResourceFactory.build(SpringApi.class, StandardCharsets.UTF_8.name());
        SPRING_RESOURCE_FACTORY_THREAD_LOCAL.set(springResource);
    }

    @Override
    protected void destroy() {
        super.destroy();
        JERSEY_RESOURCE_FACTORY_THREAD_LOCAL.remove();
        SPRING_RESOURCE_FACTORY_THREAD_LOCAL.remove();
    }

    @Override
    protected Class<?>[] getSource() {
        Class<?>[] sources = CollectionUtil.addTo(SpringApi.class, super.getSource());
        sources = CollectionUtil.addTo(JerseyResource.class, sources);
        return sources;
    }

    /**
     * 测试{@link Resource#hello(String)}方法
     * 
     * @param resource
     *            Resource
     */
    private void doTestHello(Resource resource) {
        String name = "joe";
        String result = resource.hello(name);
        Assert.assertEquals(result, name);
    }

    /**
     * 测试{@link Resource#size(List)}和{@link Resource#size(Map)}方法
     * 
     * @param resource
     *            Resource
     */
    private void doTestSize(Resource resource) {
        Map<String, Object> map = new HashMap<>();
        map.put("123", 123);
        map.put("456", 456);
        Assert.assertEquals(resource.size(map), map.size());

        List<String> list = new ArrayList<>(3);
        list.add("1");
        list.add("2");
        list.add("3");
        Assert.assertEquals(resource.size(list), list.size());
    }

    /**
     * 测试{@link Resource#user(User)}方法
     * 
     * @param resource
     *            Resource
     */
    private void doTestUser(Resource resource) {
        User user = new User();
        user.setAge(1);
        user.setName("joe");
        user.setSex("男");
        Assert.assertEquals(resource.user(user), user);
    }

    /**
     * 测试{@link Resource#formUser(String, Integer, String)}方法
     * 
     * @param resource
     *            Resource
     */
    private void doTestFormUser(Resource resource) {
        User user = new User();
        user.setAge(1);
        user.setName("joe");
        // 注意，因为sex是在header，所以这里应该使用英语，不能使用中文，否则会乱码（header不支持中文）
        user.setSex("man");
        Assert.assertEquals(resource.formUser(user.getName(), user.getAge(), user.getSex()), user);
    }

    /**
     * 测试服务器异常场景
     * 
     * @param resource
     *            Resource
     */
    private void doTestException(Resource resource) {
        ServerException exception = null;
        try {
            resource.exception();
        } catch (ServerException e) {
            exception = e;
        }
        Assert.assertNotNull(exception);
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

        @RequestMapping(value = "exception", consumes = IHttpRequestBase.CONTENT_TYPE_FORM)
        @Override
        public void exception() {
            throw new NullPointerException("name不能为空");
        }
    }

    public interface Resource {
        /**
         * 简单类型测试
         * 
         * @param name
         *            传入参数
         * @return 传入参数原路返回
         */
        String hello(String name);

        /**
         * 测试List
         * 
         * @param data
         *            传入参数
         * @return 传入List的长度
         */
        int size(List<String> data);

        /**
         * 测试Map
         * 
         * @param data
         *            传入参数
         * @return 传入Map的长度
         */
        int size(Map<String, Object> data);

        /**
         * 测试复杂类型
         * 
         * @param user
         *            传入user
         * @return 传入user原样返回
         */
        User user(User user);

        /**
         * 传入User的json数据返回User对象
         * 
         * @param name
         *            名字
         * @param age
         *            年龄
         * @param sex
         *            性别
         * @return 构建的user对象
         */
        User formUser(String name, Integer age, String sex);

        /**
         * 抛出异常
         */
        void exception();
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
