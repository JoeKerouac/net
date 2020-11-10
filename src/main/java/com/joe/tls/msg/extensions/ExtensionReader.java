package com.joe.tls.msg.extensions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joe.tls.SignatureAndHashAlgorithm;
import com.joe.ssl.message.WrapedInputStream;
import com.joe.tls.util.ByteBufferUtil;

/**
 * 扩展读取
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-12 10:00
 */
public class ExtensionReader {

    private static final Map<ExtensionType, HelloExtensionReader> READER_MAP     = new HashMap<>();

    private static final HelloExtensionReader                     UNKNOWN_READER = new UnknownExtensionReader();

    static {
        register(new EllipticCurvesExtensionReader());
        register(new EllipticPointFormatsExtensionReader());
        register(new ExtendedMasterSecretExtensionReader());
        register(new RenegotiationInfoExtensionReader());
        register(new ServerNameExtensionReader());
        register(new SignatureAndHashAlgorithmExtensionReader());
    }

    /**
     * 从输入流读取扩展数据，此时数据流起始位置应该是扩展数据的总长度字段开始处
     * 
     * @param inputStream 输入流
     * @return 读到的扩展数据
     * @throws IOException IO异常
     */
    @Deprecated
    public static List<HelloExtension> read(WrapedInputStream inputStream) throws IOException {
        List<HelloExtension> extensions = new ArrayList<>();
        // 总长度
        int len = inputStream.readInt16();
        while (len > 0) {
            // 读取2byte类型
            int extensionType = inputStream.readInt16();
            // 路由读取
            HelloExtensionReader reader = READER_MAP.getOrDefault(ExtensionType.get(extensionType),
                UNKNOWN_READER);
            HelloExtension extension = reader.read(extensionType, inputStream);
            extensions.add(extension);
            len -= extension.size();
        }
        return extensions;
    }

    /**
     * 从ByteBuffer读取扩展数据，此时ByteBuffer起始位置应该是扩展数据的总长度字段开始处
     *
     * @param buffer ByteBuffer
     * @return 读到的扩展数据
     */
    public static List<HelloExtension> read(ByteBuffer buffer) {
        List<HelloExtension> extensions = new ArrayList<>();
        // 总长度
        int len = ByteBufferUtil.mergeReadInt16(buffer);
        while (len > 0) {
            // 读取2byte类型
            int extensionType = ByteBufferUtil.mergeReadInt16(buffer);
            // 路由读取
            HelloExtensionReader reader = READER_MAP.getOrDefault(ExtensionType.get(extensionType),
                UNKNOWN_READER);
            HelloExtension extension = reader.read(extensionType, buffer);
            extensions.add(extension);
            len -= extension.size();
        }
        return extensions;
    }

    private static void register(HelloExtensionReader reader) {
        READER_MAP.put(reader.readableType(), reader);
    }

    interface HelloExtensionReader {

        /**
         * 可读取的扩展类型
         * 
         * @return 扩展类型
         */
        ExtensionType readableType();

        /**
         * 从流中读取扩展，流应该从实际内容开始
         * 
         * @param extensionType 扩展类型
         * @param inputStream 输入流
         * @return 读取到的HelloExtension
         * @throws IOException IO异常
         */
        @Deprecated
        HelloExtension read(int extensionType, WrapedInputStream inputStream) throws IOException;

        HelloExtension read(int extensionType, ByteBuffer buffer);
    }

    private static class UnknownExtensionReader implements HelloExtensionReader {
        @Override
        public ExtensionType readableType() {
            return null;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            return new UnknownExtension(extensionType, inputStream.read(inputStream.readInt16()));
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            return new UnknownExtension(extensionType, ByteBufferUtil.getInt16(buffer));
        }
    }

    private static class EllipticCurvesExtensionReader implements HelloExtensionReader {

        @Override
        public ExtensionType readableType() {
            return ExtensionType.EXT_ELLIPTIC_CURVES;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            // 抛弃总长度
            inputStream.skip(2);
            int[] curve = new int[inputStream.readInt16() / 2];
            for (int i = 0; i < curve.length; i++) {
                curve[i] = inputStream.readInt16();
            }
            return new EllipticCurvesExtension(curve);
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            // 抛弃总长度
            buffer.getShort();
            int[] curve = new int[ByteBufferUtil.mergeReadInt16(buffer) / 2];
            for (int i = 0; i < curve.length; i++) {
                curve[i] = ByteBufferUtil.mergeReadInt16(buffer);
            }
            return new EllipticCurvesExtension(curve);
        }
    }

    private static class EllipticPointFormatsExtensionReader implements HelloExtensionReader {
        @Override
        public ExtensionType readableType() {
            return ExtensionType.EXT_EC_POINT_FORMATS;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            // 抛弃总长度
            inputStream.skip(2);
            byte[] format = new byte[inputStream.readInt8()];
            for (int i = 0; i < format.length; i++) {
                format[i] = (byte) inputStream.readInt8();
            }
            return new EllipticPointFormatsExtension(format);
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            // 抛弃总长度
            buffer.getShort();
            byte[] format = new byte[ByteBufferUtil.mergeReadInt8(buffer)];
            for (int i = 0; i < format.length; i++) {
                format[i] = buffer.get();
            }
            return new EllipticPointFormatsExtension(format);
        }
    }

    private static class ExtendedMasterSecretExtensionReader implements HelloExtensionReader {
        @Override
        public ExtensionType readableType() {
            return ExtensionType.EXT_EXTENDED_MASTER_SECRET;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            inputStream.skip(2);
            return new ExtendedMasterSecretExtension();
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            buffer.getShort();
            return new ExtendedMasterSecretExtension();
        }
    }

    private static class RenegotiationInfoExtensionReader implements HelloExtensionReader {

        @Override
        public ExtensionType readableType() {
            return ExtensionType.EXT_RENEGOTIATION_INFO;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            inputStream.skip(inputStream.readInt16());
            return new RenegotiationInfoExtension();
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            ByteBufferUtil.getInt16(buffer);
            return new RenegotiationInfoExtension();
        }
    }

    private static class ServerNameExtensionReader implements HelloExtensionReader {
        @Override
        public ExtensionType readableType() {
            return ExtensionType.EXT_SERVER_NAME;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            inputStream.skip(4);
            byte type = (byte) inputStream.readInt8();
            int len = inputStream.readInt16();
            return new ServerNameExtension(type, inputStream.read(len));
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            buffer.getInt();
            byte type = buffer.get();
            return new ServerNameExtension(type, ByteBufferUtil.getInt16(buffer));
        }
    }

    private static class SignatureAndHashAlgorithmExtensionReader implements HelloExtensionReader {
        @Override
        public ExtensionType readableType() {
            return ExtensionType.EXT_SIGNATURE_ALGORITHMS;
        }

        @Override
        public HelloExtension read(int extensionType,
                                   WrapedInputStream inputStream) throws IOException {
            inputStream.skip(2);
            int len = inputStream.readInt16() / 2;
            Map<Integer, SignatureAndHashAlgorithm> allSupports = SignatureAndHashAlgorithm
                .getAllSupports();
            List<SignatureAndHashAlgorithm> supports = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                supports.add(allSupports.get(inputStream.readInt16()));
            }
            return new SignatureAndHashAlgorithmExtension(supports);
        }

        @Override
        public HelloExtension read(int extensionType, ByteBuffer buffer) {
            buffer.getShort();
            int len = ByteBufferUtil.mergeReadInt16(buffer) / 2;
            Map<Integer, SignatureAndHashAlgorithm> allSupports = SignatureAndHashAlgorithm
                .getAllSupports();
            List<SignatureAndHashAlgorithm> supports = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                supports.add(allSupports.get(ByteBufferUtil.mergeReadInt16(buffer)));
            }
            return new SignatureAndHashAlgorithmExtension(supports);
        }
    }

}
