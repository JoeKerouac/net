package com.joe.ssl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.crypto.CipherSpi;
import com.joe.ssl.crypto.DigestSpi;
import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.ssl.crypto.PhashSpi;
import com.joe.ssl.crypto.impl.BCECDHKeyExchangeSpi;
import com.joe.ssl.example.ECDHKeyPair;
import com.joe.ssl.message.*;
import com.joe.ssl.openjdk.ssl.CipherSuiteList;
import com.joe.tls.enums.ContentType;
import com.joe.tls.enums.HandshakeType;
import com.joe.tls.enums.NamedCurve;
import com.joe.tls.msg.extensions.ExtensionType;
import com.joe.utils.collection.CollectionUtil;
import com.joe.utils.common.Assert;
import com.joe.utils.concurrent.ThreadUtil;
import com.joe.utils.protocol.DatagramUtil;

/**
 * JDK自带：sun.security.ssl.ClientHandshaker
 * <p>
 * JDK自带交换计算密钥：sun.security.ec.ECDHKeyAgreement#engineGenerateSecret(java.lang.String)
 * <p>
 * bouncycastle：org.bouncycastle.crypto.tls.TlsProtocolHandler
 *
 * @author JoeKerouac
 * @version 2020年06月27日 17:05
 */
public class ClientHandshaker {

    /**
     * 服务端密钥交换公钥
     */
    private byte[]             serverAgreePublicKey;

    /**
     * 客户端密钥交换公私钥对
     */
    private ECDHKeyPair        ecdhKeyPair;

    private SecureRandom       secureRandom   = new SecureRandom();

    /**
     * 客户端随机数
     */
    private byte[]             clientRandom;

    private byte[]             preMasterKey;

    /**
     * 主密钥
     */
    private byte[]             masterSecret;

    private ServerHello        serverHello;

    private CertificateMsg     certificateMsg;

    /**
     * 加密套件
     */
    private CipherSuiteList    cipherSuiteList;

    private InputRecord        inputRecord;

    private OutputRecord       outputRecord;

    // 摘要不对header摘要（最外层）
    private DigestSpi          digestSpi;
    private PhashSpi           phashSpi;

    private CipherSpi          readCipher;

    private CipherSpi          writeCipher;

    private byte[]             clientWriteCipherKey;
    private byte[]             clientWriteIv;
    private byte[]             clientWriteMac;

    private byte[]             clientReadCipherKey;
    private byte[]             clientReadIv;
    private byte[]             clientReadMac;

    /**
     * ECDH密钥交换算法
     */
    private ECDHKeyExchangeSpi keyExchangeSpi = new BCECDHKeyExchangeSpi();

    /**
     * 处理握手数据，握手数据应该从Handshake的type开始，也就是包含完整的Handshake数据（不是record）
     *
     * @param handshakeData 握手数据
     */
    public void process(byte[] handshakeData) throws Exception {
        WrapedInputStream inputStream = new WrapedInputStream(
            new ByteArrayInputStream(handshakeData));

        if (digestSpi != null) {
            digestSpi.update(handshakeData);
        }

        int typeId = inputStream.readInt8();
        HandshakeType type = HandshakeType.getByCode(typeId);
        if (type == null) {
            throw new RuntimeException(String.format("不支持的握手类型：%d", typeId));
        }

        int bodyLen = inputStream.readInt24();
        System.out.println("收到\"" + type + "\"类型的握手数据，长度是：" + bodyLen);
        switch (type) {
            case SERVER_HELLO:
                this.serverHello = new ServerHello();
                this.serverHello.init(bodyLen, inputStream);
                // 初始化摘要器
                String hashAlgorithm = this.serverHello.getCipherSuite().getHashDesc().getHashAlg();
                this.digestSpi = DigestSpi.getInstance(hashAlgorithm);
                this.phashSpi = PhashSpi.getInstance(hashAlgorithm);
                // 补上client_hello的摘要
                System.out.println("补充上client_hello的数据");
                this.digestSpi.update(outputRecord.getStream().toByteArray());
                System.out.println("补充上server_hello的摘要");
                // 补上本次server_hello的摘要
                digestSpi.update(handshakeData);
                outputRecord.setStream(null);
                inputRecord.setDigestSpi(digestSpi);
                outputRecord.setDigestSpi(digestSpi);
                break;
            case CERTIFICATE:
                // 这里先不管证书，采用ECC相关算法时证书只用来签名
                this.certificateMsg = new CertificateMsg();
                this.certificateMsg.init(bodyLen, inputStream);
                break;
            case SERVER_KEY_EXCHANGE:
                // 处理服务端的密钥交换
                int curveType = inputStream.readInt8();
                // 这个必须等于3，其他不处理，目前应该也不会有其他的值
                Assert.isTrue(curveType == 3);
                int curveId = inputStream.readInt16();

                ECDomainParameters domainParameters = NamedCurve.getECParameters(curveId);

                // 如果等于null表示不支持
                if (domainParameters == null) {
                    throw new RuntimeException(String.format("不支持的椭圆曲线id：%d", curveId));
                }

                int publicKeyLen = inputStream.readInt8();
                this.serverAgreePublicKey = inputStream.read(publicKeyLen);
                System.out.println("服务端公钥为：" + Arrays.toString(serverAgreePublicKey));
                System.out.println("curveId为:" + curveId);

                // 初始化本地公私钥，用于后续密钥交换
                this.ecdhKeyPair = keyExchangeSpi.generate(curveId);

                // 计算preMasterKey
                this.preMasterKey = keyExchangeSpi.keyExchange(serverAgreePublicKey,
                    ecdhKeyPair.getPrivateKey(), curveId);
                System.out.println("计算出pre:" + Arrays.toString(preMasterKey));

                // 验签
                Signature signature = SignatureAndHashAlgorithm
                    .newSignatureAndHash(inputStream.readInt16());
                // 服务端的签名
                byte[] serverSignData = inputStream.read(inputStream.readInt16());
                signature.initVerify(this.certificateMsg.getPublicKey());
                signature.update(clientRandom);
                signature.update(serverHello.getServerRandom());
                // CURVE_NAMED_CURVE
                signature.update((byte) curveType);
                signature.update((byte) (curveId >> 8));
                signature.update((byte) curveId);
                signature.update((byte) publicKeyLen);
                signature.update(serverAgreePublicKey);

                if (!signature.verify(serverSignData)) {
                    throw new RuntimeException("签名验证失败");
                } else {
                    System.out.println("签名验证成功");
                }

                break;
            case SERVER_HELLO_DONE:

                // 准备发送client key exchange消息
                ECDHClientKeyExchange ecdhClientKeyExchange = new ECDHClientKeyExchange(
                    ecdhKeyPair.getPublicKey());
                write(ecdhClientKeyExchange, outputRecord);
                System.out.println("ECDHClientKeyExchange消息发送完毕");
                // 这个消息不应该被摘要
                sendChangeCipher(outputRecord);
                System.out.println("changeCipherSpec消息发送完毕");

                // 判断serverHello中有没有包含extended_master_secret扩展，因为master_secret的具体算法跟这个相关
                byte[] sessionHash = null;
                if (serverHello.getExtension(ExtensionType.EXT_EXTENDED_MASTER_SECRET) != null) {
                    sessionHash = digestSpi.copy().digest();
                }

                masterSecret = calcMaster(phashSpi, preMasterKey, sessionHash, clientRandom,
                    serverHello.getServerRandom());
                System.out.println("生成的masterSecret是：" + Arrays.toString(masterSecret));

                // 初始化cipher
                CipherSuite.CipherDesc cipherDesc = this.serverHello.getCipherSuite().getCipher();
                int macLen = this.serverHello.getCipherSuite().getMacDesc().getMacLen();
                int cipherKeyLen = cipherDesc.getKeySize();
                // 对于AEAD模式实际上是这个
                int ivLen = cipherDesc.getFixedIvLen();

                // 长度macLen的clientMacKey、serverMacKey，长度cipherKeyLen的clientCipherKey、serverCipherKey
                // 长度ivLen的clientIv、serverIv
                byte[] output = new byte[(macLen + cipherKeyLen + ivLen) * 2];
                phashSpi.init(masterSecret);
                // 通过PRF算法计算
                phashSpi.phash(CollectionUtil.merge("key expansion".getBytes(),
                    CollectionUtil.merge(this.serverHello.getServerRandom(), this.clientRandom)),
                    output);
                System.out.println("连接参数：" + Arrays.toString(output));
                this.clientWriteMac = new byte[macLen];
                this.clientWriteCipherKey = new byte[cipherKeyLen];
                this.clientWriteIv = new byte[ivLen];

                System.arraycopy(output, 0, this.clientWriteMac, 0, macLen);
                System.arraycopy(output, 2 * macLen, this.clientWriteCipherKey, 0, cipherKeyLen);
                System.arraycopy(output, 2 * (macLen + cipherKeyLen), this.clientWriteIv, 0, ivLen);

                // TODO 补全cipher初始化逻辑，对于AEAD模式需要晚些针对每次请求都重新初始化
                this.writeCipher = CipherSpi.getInstance(cipherDesc);
                // 如果不是AEAD模式，则直接初始化，否则稍后再初始化
                if (cipherDesc.getCipherType() != CipherSuite.CipherType.AEAD) {
                    this.writeCipher.init(this.clientWriteCipherKey, this.clientWriteIv,
                        CipherSpi.ENCRYPT_MODE);
                }

                //---------------------------------------------

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // TODO 现在应该就差数据可能是错误的了
                new Finished(digestSpi, phashSpi, masterSecret, true)
                    .write(new WrapedOutputStream(outputStream));
                byte[] data = outputStream.toByteArray();

                // 实际上AEAD模式下就是sequenceNumber，第一个sequence就是0，这里先不管，因为目前只发送一个消息
                // sequence是固定的8byte
                byte[] nonce = new byte[8];

                System.out.println("计算出clientWriteIv:" + Arrays.toString(clientWriteIv));
                System.out
                    .println("计算出clientWriteCipherKey:" + Arrays.toString(clientWriteCipherKey));
                // 初始化AEAD模式的cipher
                if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
                    // iv总长度12
                    byte[] iv = new byte[12];
                    System.arraycopy(this.clientWriteIv, 0, iv, 0, this.clientWriteIv.length);
                    // 对于AEAD模式，这个实际上是sequenceNumber，第一次是0，所以这里先写死不初始化
                    System.arraycopy(nonce, 0, iv, this.clientWriteIv.length, nonce.length);

                    this.writeCipher.init(this.clientWriteCipherKey, iv, CipherSpi.ENCRYPT_MODE);
                    // 认证添加数据,目前是第一个消息，就是0不用管sequenceNumber，sequence number + record type + protocol version + record length
                    byte[] aadData = new byte[8 + 1 + 2 + 2];
                    // type
                    aadData[8] = (byte) ContentType.HANDSHAKE.getCode();
                    aadData[9] = (byte) TlsVersion.TLS1_2.getMajorVersion();
                    aadData[10] = (byte) TlsVersion.TLS1_2.getMinorVersion();
                    // 长度
                    aadData[11] = (byte) (data.length >> 8);
                    aadData[12] = (byte) data.length;

                    this.writeCipher.updateAAD(aadData);

                }

                data = writeCipher.doFinal(data);

                outputRecord.writeInt8(ContentType.HANDSHAKE.getCode());
                TlsVersion.TLS1_2.write(outputRecord);
                outputRecord.writeInt16(data.length + 8);

                outputRecord.write(nonce);
                outputRecord.write(data);
                outputRecord.flush();
                System.out.println("写出最终数据长度:" + (data.length + 8));

                System.out.println("client finish消息发送完毕");
                break;
        }
    }

    private void acquireAuthenticationBytes() {

    }

    private static void write(HandshakeMessage message,
                              OutputRecord outputStream) throws IOException {
        outputStream.write(message);
        outputStream.flush();
        //        outputStream.writeInt8(ContentType.HANDSHAKE.getCode());
        //        TlsVersion.TLS1_2.write(outputStream);
        //        outputStream.writeInt16(message.size());
        //        message.write(outputStream);
        //        outputStream.flush();
    }

    private static void sendChangeCipher(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt8(ContentType.CHANGE_CIPHER_SPEC.getCode());
        TlsVersion.TLS1_2.write(outputStream);
        outputStream.writeInt16(1);
        outputStream.writeInt8(1);
    }

    public static void main(String[] args) throws Exception {
        // ip.src == 39.156.66.14 || ip.dst == 39.156.66.14
        //        Socket socket = new Socket("192.168.1.111", 12345);
        Security.addProvider(new BouncyCastleProvider());

        //        Socket socket = new Socket("39.156.66.14", 443);
        Socket socket = new Socket("127.0.0.1", 12345);

        OutputRecord outputStream = new OutputRecord(socket.getOutputStream());

        ClientHello hello = new ClientHello("baidu.com");
        write(hello, outputStream);

        InputRecord inputStream = new InputRecord(socket.getInputStream());
        ClientHandshaker handshaker = new ClientHandshaker();
        handshaker.inputRecord = inputStream;
        handshaker.outputRecord = outputStream;
        handshaker.clientRandom = hello.getClientRandom();

        while (true) {
            ThreadUtil.sleep(1);
            int contentType = inputStream.read();
            ContentType type = EnumInterface.getByCode(contentType, ContentType.class);
            System.out.println("contentType:" + type + " : " + contentType);
            if (type != ContentType.HANDSHAKE && type != ContentType.CHANGE_CIPHER_SPEC) {
                throw new RuntimeException("不支持的content type:" + type);
            }
            int version = inputStream.readInt16();
            System.out.printf("version: %x%n", version);
            int len = inputStream.readInt16();
            System.out.println("len:" + len);
            System.out.println("可用：" + inputStream.available());
            byte[] data = inputStream.read(len);
            System.out.println("实际读取报文：" + Arrays.toString(data));
            System.out.println("可用：" + inputStream.available());

            // 实际数据的起始位置
            int contentOffset = 0;

            // 有可能一次性多条数据，多条数据共用一个record头，直接从content开始
            while (contentOffset < data.length) {
                System.out.println("type:" + data[contentOffset]);
                byte[] contentLenData = new byte[4];
                contentLenData[1] = data[contentOffset + 1];
                contentLenData[2] = data[contentOffset + 2];
                contentLenData[3] = data[contentOffset + 3];
                int contentLen = DatagramUtil.mergeToInt(contentLenData, 0);
                //                System.out.println("本次读取报文：" + Arrays.toString(Arrays.copyOfRange(data, contentOffset, contentLen + 4)));

                handshaker.process(
                    Arrays.copyOfRange(data, contentOffset, contentOffset + contentLen + 4));
                contentOffset = contentOffset + contentLen + 4;

                System.out.println("\n\n");
            }
        }
    }

    /**
     * 计算masterKey
     * @param phashSpi PhashSpi
     * @param preMasterKey preMasterKey
     * @param sessionHash sessionHash（对除了finished外的所有握手消息进行摘要），如果客户端和服务端使用extended_master_secret扩
     *                    展，那么不应该为空
     * @param clientRandom 客户端随机数
     * @param serverRandom 服务端随机数
     * @return master_secret
     */
    private byte[] calcMaster(PhashSpi phashSpi, byte[] preMasterKey, byte[] sessionHash,
                              byte[] clientRandom, byte[] serverRandom) {
        byte[] label;
        byte[] seed;
        if (sessionHash != null && sessionHash.length > 0) {
            // 当有ExtendedMasterSecretExtension扩展的时候需要用extended master secret作为label，同时算法也略微有所不同
            // 详情参见rfc7627
            label = "extended master secret".getBytes();
            seed = sessionHash;
        } else {
            label = "master secret".getBytes();
            seed = CollectionUtil.merge(clientRandom, serverRandom);
        }

        byte[] masterSecret = new byte[48];
        phashSpi.init(preMasterKey);
        phashSpi.phash(CollectionUtil.merge(label, seed), masterSecret);
        return masterSecret;
    }

    /**
     * 生成非对称加密密钥
     *
     * @param ecParams EC加密参数
     * @return 根据EC加密参数得到的非对称加密密钥
     */
    protected AsymmetricCipherKeyPair generateECKeyPair(ECDomainParameters ecParams) {
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(ecParams,
            secureRandom);
        keyPairGenerator.init(keyGenerationParameters);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 根据自己的私钥和对方的公钥计算PremasterSecret，进而计算MasterSecret
     *
     * @param publicKey  公钥
     * @param privateKey 私钥
     * @return PremasterSecret
     */
    protected byte[] calculateECDHBasicAgreement(ECPublicKeyParameters publicKey,
                                                 ECPrivateKeyParameters privateKey) {
        ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(privateKey);
        BigInteger agreement = basicAgreement.calculateAgreement(publicKey);
        return BigIntegers.asUnsignedByteArray(agreement);
    }
}
