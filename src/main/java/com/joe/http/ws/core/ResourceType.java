package com.joe.http.ws.core;

import com.joe.utils.reflect.clazz.ClassUtils;

/**
 * 资源类型
 *
 * @author joe
 * @version 2018.08.21 14:24
 */
public enum ResourceType {
    JERSEY("com.joe.http.ws.core.JerseyResourceAnalyze"), SPRING("com.joe.http.ws.core.SpringResourceAnalyze");

    private Class<? extends ResourceAnalyze> clazz;

    @SuppressWarnings("unchecked")
    ResourceType(String className) {
        this.clazz = ClassUtils.loadClass(className);
    }

    /**
     * 获取ResourceAnalyze的实际类型
     * 
     * @return ResourceAnalyze的实际类型
     */
    public Class<? extends ResourceAnalyze> getResourceAnalyzeClass() {
        return this.clazz;
    }
}
