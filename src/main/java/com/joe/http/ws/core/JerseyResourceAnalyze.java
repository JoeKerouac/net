package com.joe.http.ws.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import com.joe.http.ws.exception.WsException;
import com.joe.utils.common.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * jersey资源分析，不支持@BeanParam
 *
 * @author joe
 * @version 2018.08.21 14:23
 */
@Slf4j
public final class JerseyResourceAnalyze implements ResourceAnalyze {

    /**
     * 资源实例class
     */
    private final Class<?>  resourceClass;

    /**
     * 资源method
     */
    private final Method    method;

    /**
     * 资源方法参数
     */
    private final Object[]  args;

    /**
     * 资源前缀
     */
    private String          pathPrefix;

    /**
     * 资源后缀
     */
    private String          pathLast;

    /**
     * 资源参数
     */
    private ResourceParam[] params = ResourceAnalyze.EMPTY;

    /**
     * 资源方法
     */
    private ResourceMethod  resourceMethod;

    /**
     * 请求contentType
     */
    private String[]          requestContentTypes;

    /**
     * 响应contentType
     */
    private String[]          responseContentTypes;

    /**
     * 是否是一个资源
     */
    private boolean         isResource;

    JerseyResourceAnalyze(Class<?> resourceClass, Method method, Object[] args) {
        Assert.notNull(resourceClass, "resourceClass must not be null");
        Assert.notNull(method, "method must not be null");
        if (method.getDeclaringClass() != resourceClass) {
            throw new IllegalArgumentException("指定method不是resourceClass中声明的");
        }
        this.resourceClass = resourceClass;
        this.method = method;
        this.args = args;

        // 初始化
        init();
    }

    private void init() {
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
            if (method.getAnnotation(Produces.class) != null) {
                responseContentTypes = method.getAnnotation(Produces.class).value();
            }

            if (method.getAnnotation(Consumes.class) != null) {
                requestContentTypes = method.getAnnotation(Consumes.class).value();
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

    /**
     * 是否是资源
     * @return true表示是资源，false表示不是
     */
    @Override
    public boolean isResource() {
        return isResource;
    }

    /**
     * path前缀
     * @return path前缀
     */
    @Override
    public String pathPrefix() {
        return pathPrefix;
    }

    /**
     * path结尾
     * @return path结尾
     */
    @Override
    public String pathLast() {
        return pathLast;
    }

    /**
     * 获取参数列表
     * @return 参数列表
     */
    @Override
    public ResourceParam[] getParams() {
        return params;
    }

    /**
     * 获取请求方法
     * @return 请求方法
     */
    @Override
    public ResourceMethod getResourceMethod() {
        return resourceMethod;
    }

    @Override
    public String[] getRequestContentTypes() {
        return requestContentTypes;
    }

    @Override
    public String[] getResponseContentTypes() {
        return responseContentTypes;
    }
}
