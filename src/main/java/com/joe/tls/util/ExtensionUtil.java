package com.joe.tls.util;

import java.nio.ByteBuffer;
import java.util.Collection;

import com.joe.tls.msg.extensions.HelloExtension;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-12 15:12
 */
public class ExtensionUtil {

    /**
     * 写出扩展
     * 
     * @param extensions
     *            要写出的扩展
     * @param buffer
     *            buffer
     */
    public static void writeExtensions(Collection<HelloExtension> extensions, ByteBuffer buffer) {
        int size = extensions.stream().mapToInt(HelloExtension::size).sum();
        ByteBufferUtil.writeInt16(size, buffer);
        extensions.forEach(extension -> extension.write(buffer));
    }

}
