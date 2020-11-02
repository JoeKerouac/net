package com.joe.tls.msg.extensions;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.joe.ssl.message.WrapedOutputStream;
import com.joe.tls.util.ByteBufferUtil;

/**
 * 该扩展定义master_secret的计算方式:
 * <p>
 *     master_secret = PRF(pre_master_secret, "extended master secret", session_hash)
 * </p>
 * <p></p>
 * <p>
 *     主要是为安全考虑，原来master_secret是跟上下文无关的，所以很容易被中间人攻击，现在将其更改为和上下文相关的；ClientHello和ServerHello必须
 *     都包含该扩展才会最终应用本扩展；如果ClientHello中没有该扩展，那么ServerHello中也不应该包含；
 * </p>
 * <p></p>
 * <p>
 *     考虑这样一个场景：C和S通信，A作为中间人，只需要拦截C-S的握手，并在两边使用相同的master_secret，后续就算C-S重新握手，A也能随时监听，同
 *     时C-S也不能发现问题，即使A退出监听；所以引入了该算法，保证就算有中间人，C-A、A-S的master_secret也不一致，这样一旦A退出，C-S马上就能
 *     发现问题；
 * </p>
 * <p></p>
 * <p>
 *     具体操作：使用extended master secret代替master secret，使用session_hash代替ClientHello.random和ServerHello.random；<br/>
 *     session_hash = Hash(handshake_messages)
 * </p>
 *
 * https://tools.ietf.org/html/rfc7627
 */
public class ExtendedMasterSecretExtension implements HelloExtension {

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(0);
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        ByteBufferUtil.writeInt16(getExtensionType().id, buffer);
        ByteBufferUtil.writeInt16(0, buffer);
    }

    @Override
    public int size() {
        return 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_EXTENDED_MASTER_SECRET;
    }

    @Override
    public String toString() {
        return String.format("%s", getExtensionType().name);
    }
}
