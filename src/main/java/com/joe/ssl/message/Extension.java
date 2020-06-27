package com.joe.ssl.message;

import lombok.Data;

/**
 * extension格式如下：
 * <li>2byte的类型</li>
 * <li>2byte长度</li>
 * <li>实际内容，边长，长度与上边定义的相同</li>
 *
 * @author JoeKerouac
 * @version 2020年06月13日 16:44
 */
@Data
public class Extension<T> {

    private ExtensionType type;

    private int           len;

    private T             body;
}
