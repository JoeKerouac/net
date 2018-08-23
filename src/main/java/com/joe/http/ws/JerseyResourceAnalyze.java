package com.joe.http.ws;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import lombok.extern.slf4j.Slf4j;

/**
 * jersey资源分析，不支持@BeanParam
 *
 * @author joe
 * @version 2018.08.21 14:23
 */
@Slf4j
public class JerseyResourceAnalyze extends ResourceAnalyze {
    JerseyResourceAnalyze(Class<?> resourceClass, Object resourceInstance, Method method,
                                 Object[] args) {
        super(resourceClass, resourceInstance, method, args);
    }

    @Override
    public void init() {
        {
            //解析路径
            Path prePath = resourceClass.getDeclaredAnnotation(Path.class);
            if (prePath == null) {
                isResource = false;
                return;
            }

            Path namePath = method.getAnnotation(Path.class);
            if (namePath == null) {
                isResource = false;
                return;
            }
            isResource = true;

            pathPrefix = prePath.value();
            log.debug("请求的前缀是：{}", pathPrefix);
            pathLast = namePath.value();
            log.debug("接口名是：{}", pathLast);
        }

        {
            //解析请求方法
            if (method.getAnnotation(POST.class) != null) {
                resourceMethod = ResourceMethod.POST;
            } else if (method.getAnnotation(GET.class) != null) {
                resourceMethod = ResourceMethod.GET;
            } else if (method.getAnnotation(PUT.class) != null) {
                resourceMethod = ResourceMethod.PUT;
            } else if (method.getAnnotation(DELETE.class) != null) {
                resourceMethod = ResourceMethod.DELETE;
            } else {
                throw new WsException("未知请求方法");
            }
        }

        {
            //解析参数
            Parameter[] parameters = method.getParameters();
            int len = parameters.length;
            if (len > 0) {
                params = new ResourceParam[len];

                for (int i = 0; i < len; i++) {
                    //参数值
                    Object value = args[i];
                    Parameter parameter = parameters[i];

                    //资源参数
                    ResourceParam param = new ResourceParam();
                    param.setParam(value);
                    param.setIndex(i);
                    params[i] = param;

                    //注解
                    Annotation[] annotations = parameter.getAnnotations();

                    //解析注解
                    for (Annotation annotation : annotations) {
                        if (annotation instanceof QueryParam) {
                            QueryParam queryParam = (QueryParam) annotation;
                            log.debug("参数是QueryParam，参数名为：{}，参数值为：{}", queryParam.value(), value);
                            param.setType(ResourceParam.Type.QUERY);
                            param.setName(queryParam.value());
                            break;
                        } else if (annotation instanceof HeaderParam) {
                            //解析headerparam
                            HeaderParam headerParam = (HeaderParam) annotation;
                            log.debug("参数是HeaderParam，参数名为：{}，参数值为：{}", headerParam.value(), value);
                            param.setType(ResourceParam.Type.HEADER);
                            param.setName(headerParam.value());
                            break;
                        } else if (annotation instanceof PathParam) {
                            PathParam pathParam = (PathParam) annotation;
                            log.debug("参数是PathParam，参数名为：{}，参数值为：{}", pathParam.value(), value);
                            param.setType(ResourceParam.Type.PATH);
                            param.setName(pathParam.value());
                            break;
                        } else if (annotation instanceof FormParam) {
                            FormParam formParam = (FormParam) annotation;
                            log.debug("参数是FormParam，参数名为：{}，参数值为：{}", formParam.value(), value);
                            param.setType(ResourceParam.Type.FORM);
                            param.setName(formParam.value());
                            break;
                        } else if (annotation instanceof Context) {
                            //当前是需要context的，跳过
                            param.setType(ResourceParam.Type.CONTEXT);
                            break;
                        } else if (annotation instanceof BeanParam) {
                            throw new WsException("不支持的注解类型：@BeanParam");
                        }
                    }

                    //如果到这里仍然没有类型，那么设置为json
                    if (param.getType() == null) {
                        param.setType(ResourceParam.Type.JSON);
                    }
                }
            }

        }
    }
}
