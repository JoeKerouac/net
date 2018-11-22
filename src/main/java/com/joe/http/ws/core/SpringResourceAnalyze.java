package com.joe.http.ws.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.joe.http.exception.NetException;
import com.joe.utils.common.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 如果参数有自定义对象那么最多有一个非context参数，也就是只能又一个自定义对象，不能有其他参数
 *
 * @author joe
 * @version 2018.08.21 15:53
 */
@Slf4j
public class SpringResourceAnalyze extends ResourceAnalyze {

    SpringResourceAnalyze(Class<?> resourceClass, Method method, Object[] args) {
        super(resourceClass, method, args);
    }

    @Override
    public void init() {
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        {
            //解析路径
            if (resourceClass.getDeclaredAnnotation(Controller.class) == null) {
                isResource = false;
                return;
            }

            RequestMapping prePath = resourceClass.getDeclaredAnnotation(RequestMapping.class);

            if (mapping == null) {
                isResource = false;
                return;
            }
            isResource = true;

            pathPrefix = prePath == null ? "" : prePath.value()[0];
            log.debug("请求的前缀是：{}", pathPrefix);
            pathLast = mapping.value()[0];
            log.debug("接口名是：{}", pathLast);

            //解析请求方法
            RequestMethod[] methods = mapping.method();
            if (methods.length == 0) {
                resourceMethod = ResourceMethod.POST;
            } else {
                for (RequestMethod method : methods) {
                    if (method == RequestMethod.POST) {
                        resourceMethod = ResourceMethod.POST;
                        break;
                    } else if (method == RequestMethod.PUT) {
                        resourceMethod = ResourceMethod.PUT;
                        break;
                    } else if (method == RequestMethod.DELETE) {
                        resourceMethod = ResourceMethod.DELETE;
                    } else if (method == RequestMethod.GET) {
                        resourceMethod = ResourceMethod.GET;
                    }
                }
            }
        }

        {
            //解析参数
            Parameter[] parameters = method.getParameters();
            int len = parameters.length;
            if (len > 0) {
                ParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
                String[] paramNames = discoverer.getParameterNames(method);
                params = new ResourceParam[len];

                for (int i = 0; i < len; i++) {
                    //参数值
                    Object value = args[i];
                    Parameter parameter = parameters[i];
                    Class<?> parameterType = parameter.getType();

                    //资源参数
                    ResourceParam param = new ResourceParam();
                    param.setParam(value);
                    param.setIndex(i);
                    params[i] = param;

                    PathVariable pathVariable;
                    RequestHeader requestHeader;
                    SessionAttribute sessionAttribute;

                    if ((sessionAttribute = parameter
                        .getDeclaredAnnotation(SessionAttribute.class)) != null) {
                        param.setType(ResourceParam.Type.CONTEXT);
                        String name = StringUtils.isEmpty(sessionAttribute.name())
                            ? sessionAttribute.value()
                            : sessionAttribute.name();
                        param.setName(StringUtils.isEmpty(name) ? paramNames[i] : name);
                    } else if ((pathVariable = parameter
                        .getDeclaredAnnotation(PathVariable.class)) != null) {
                        String name = StringUtils.isEmpty(pathVariable.value())
                            ? pathVariable.name()
                            : pathVariable.value();
                        if (StringUtils.isEmpty(name)) {
                            name = paramNames[i];
                        }
                        param.setName(name);
                        param.setType(ResourceParam.Type.PATH);
                    } else if ((requestHeader = parameter
                        .getDeclaredAnnotation(RequestHeader.class)) != null) {
                        String name = StringUtils.isEmpty(requestHeader.value())
                            ? requestHeader.name()
                            : requestHeader.value();
                        if (StringUtils.isEmpty(name)) {
                            name = paramNames[i];
                        }
                        param.setName(name);

                        if (HttpHeaders.class.isAssignableFrom(parameterType)) {
                            param.setType(ResourceParam.Type.CONTEXT);
                        } else {
                            param.setType(ResourceParam.Type.HEADER);
                        }
                    } else {
                        //设置名字
                        RequestParam requestParam = parameter
                            .getDeclaredAnnotation(RequestParam.class);
                        if (requestParam != null) {
                            String name = StringUtils.isEmpty(requestParam.value())
                                ? requestParam.name()
                                : requestParam.value();
                            if (StringUtils.isEmpty(name)) {
                                name = paramNames[i];
                            }
                            param.setName(name);
                        } else {
                            param.setName(paramNames[i]);
                        }

                        //判断content-type ，默认json，然后是form，只支持这两种
                        //content-type表示该字段要从form数据取还是json数据取
                        String[] consumes = mapping.consumes();
                        boolean isJson = false;

                        // 如果既不是form又不是json同时又不是GET请求那么不支持
                        if (consumes.length == 0 || Arrays.stream(consumes)
                            .anyMatch(s -> s.contains("application/json"))) {
                            isJson = true;
                        } else if (Arrays.stream(consumes)
                            .noneMatch(s -> s.contains("application/x-www-form-urlencoded"))
                                   && ResourceMethod.GET != resourceMethod) {
                            throw new NetException(
                                "不支持的contextType : " + Arrays.toString(consumes));
                        }

                        //如果是POST那么只能是json或者form，如果是GET那么只能是QUERY
                        if (resourceMethod == ResourceMethod.POST) {
                            if (HttpSession.class.isAssignableFrom(parameterType)
                                || HttpServletRequest.class.isAssignableFrom(parameterType)
                                || HttpServletResponse.class.isAssignableFrom(parameterType)) {
                                param.setType(ResourceParam.Type.CONTEXT);
                            } else if (isJson) {
                                param.setType(ResourceParam.Type.JSON);
                            } else {
                                param.setType(ResourceParam.Type.FORM);
                            }
                        } else {
                            param.setType(ResourceParam.Type.QUERY);
                        }
                        log.debug("method[{}]的参数[{}]对应的contextType是[{}]", method, param.getName(),
                            param.getType());
                    }
                }
            }
        }

        {
            //如果没有参数那么将请求方法重新设置为GET
            long count = Stream.of(params)
                .filter(param -> !ResourceParam.Type.CONTEXT.equals(param.getType())).count();
            if (count == 0) {
                resourceMethod = ResourceMethod.GET;
            }
        }
    }
}
