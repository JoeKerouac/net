package com.joe.ssl.message.extension;

import java.io.IOException;

import com.joe.ssl.message.WrapedOutputStream;

/**
 * extension格式如下：
 * <li>2byte的类型</li>
 * <li>2byte长度，单位byte</li>
 * <li>实际内容，变长，长度与上边定义的相同</li>
 *
 * @author JoeKerouac
 * @version 2020年06月13日 16:44
 */
public interface HelloExtension {

    /**
     * 写出扩展到指定输出流
     *
     * @param outputStream 输出流
     * @throws IOException 异常
     */
    void write(WrapedOutputStream outputStream) throws IOException;

    /**
     * 该扩展的输出大小
     * @return 扩展输出总大小
     */
    int size();

    /**
     * 扩展类型
     *
     * @return 扩展类型
     */
    ExtensionType getExtensionType();
}
