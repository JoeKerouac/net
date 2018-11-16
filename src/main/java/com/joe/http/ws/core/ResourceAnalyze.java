package com.joe.http.ws.core;

import java.lang.reflect.Method;

import com.joe.utils.common.Assert;

/**
 * 资源解析
 *
 * @author joe
 * @version 2018.08.21 13:58
 */
public abstract class ResourceAnalyze {
    protected static final ResourceParam[] EMPTY  = new ResourceParam[0];
    /**
     * 资源实例class
     */
    protected final Class<?>               resourceClass;

    /**
     * 资源method
     */
    protected final Method                 method;

    /**
     * 资源方法参数
     */
    protected final Object[]               args;

    /*下列参数需要子类解析赋值*/
    /**
     * 资源前缀
     */
    protected String                       pathPrefix;

    /**
     * 资源后缀
     */
    protected String                       pathLast;

    /**
     * 资源参数
     */
    protected ResourceParam[]              params = ResourceAnalyze.EMPTY;

    /**
     * 资源方法
     */
    protected ResourceMethod resourceMethod;

    /**
     * 是否是一个资源
     */
    protected boolean                      isResource;

    /**
     * 构造器，子类必须具有与该构造器签名相同的构造器
     * @param resourceClass 资源class
     * @param method 资源方法
     * @param args 资源方法参数
     */
    public ResourceAnalyze(Class<?> resourceClass, Method method, Object[] args) {
        Assert.notNull(resourceClass, "resourceClass must not be null");
        Assert.notNull(method, "method must not be null");
        if (method.getDeclaringClass() != resourceClass) {
            throw new IllegalArgumentException("指定method不是resourceClass中声明的");
        }
        this.resourceClass = resourceClass;
        this.method = method;
        this.args = args;
        init();
    }

    /**
     * 解析参数，如果发现不是资源方法那么只需要将isResource返回false即可
     */
    public abstract void init();

    /**
     * 是否是资源
     * @return true表示是资源，false表示不是
     */
    public boolean isResource() {
        return isResource;
    }

    /**
     * path前缀
     * @return path前缀
     */
    public String pathPrefix() {
        return pathPrefix;
    }

    /**
     * path结尾
     * @return path结尾
     */
    public String pathLast() {
        return pathLast;
    }

    /**
     * 获取参数列表
     * @return 参数列表
     */
    public ResourceParam[] getParams() {
        return params;
    }

    /**
     * 获取请求方法
     * @return 请求方法
     */
    public ResourceMethod getResourceMethod() {
        return resourceMethod;
    }
}
