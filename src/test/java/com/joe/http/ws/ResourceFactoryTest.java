package com.joe.http.ws;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.http.ws.core.ResourceType;
import com.joe.utils.test.WebBaseTest;

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

    private void doTest(Resource resource) {
        String name = "joe";
        String result = resource.hello(name);
        Assert.assertEquals(result, "hello : " + name);
    }

    @Controller
    @RequestMapping("test")
    public static class SpringApi implements Resource {
        @RequestMapping(value = "hello")
        @ResponseBody
        public String hello(String name) {
            return "hello : " + name;
        }
    }

    public interface Resource {
        String hello(String name);
    }

    @Path("test")
    public interface JerseyResource extends Resource {
        @POST
        @Path("hello")
        String hello(@FormParam("name") String name);
    }
}
