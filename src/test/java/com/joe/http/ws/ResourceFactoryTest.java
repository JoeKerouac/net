package com.joe.http.ws;

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
