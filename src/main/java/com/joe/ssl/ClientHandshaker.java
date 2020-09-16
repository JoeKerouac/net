package com.joe.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Arrays;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.crypto.CipherSpi;
import com.joe.ssl.crypto.DigestSpi;
import com.joe.utils.collection.CollectionUtil;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import com.joe.ssl.crypto.PhashSpi;
import com.joe.ssl.message.*;
import com.joe.ssl.openjdk.ssl.CipherSuiteList;
import com.joe.utils.common.Assert;

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
    private ECPublicKeyParameters  ecAgreeServerPublicKey;

    /**
     * 客户端密钥交换公钥
     */
    private ECPublicKeyParameters  ecAgreeClientPublicKey;

    /**
     * 客户端密钥交换私钥
     */
    private ECPrivateKeyParameters ecAgreeClientPrivateKey;

    private SecureRandom           secureRandom = new SecureRandom();

    /**
     * 客户端随机数
     */
    private byte[]                 clientRandom;

    private byte[]                 preMasterKey;

    /**
     * 主密钥
     */
    private byte[]                 masterSecret;

    private ServerHello            serverHello;

    private CertificateMsg         certificateMsg;

    /**
     * 加密套件
     */
    private CipherSuiteList        cipherSuiteList;

    private InputRecord            inputRecord;

    private OutputRecord           outputRecord;

    private DigestSpi              digestSpi;

    private CipherSpi              readCipher;

    private CipherSpi              writeCipher;

    /**
     * 处理握手数据，握手数据应该从Handshake的type开始，也就是包含完整的Handshake数据（不是record）
     *
     * @param handshakeData 握手数据
     */
    public void process(byte[] handshakeData) throws Exception {
        WrapedInputStream inputStream = new WrapedInputStream(
            new ByteArrayInputStream(handshakeData));

        int typeId = inputStream.readInt8();
        HandshakeType type = HandshakeType.getByCode(typeId);
        if (type == null) {
            throw new RuntimeException(String.format("不支持的握手类型：%d", typeId));
        }

        int bodyLen = inputStream.readInt24();
        System.out.println("收到\"" + type + "\"类型的握手数据");
        switch (type) {
            case SERVER_HELLO:
                this.serverHello = new ServerHello();
                this.serverHello.init(bodyLen, inputStream);
                // 初始化摘要器
                String hashAlgorithm = this.serverHello.getCipherSuite().getMacDesc().getMacAlg();
                this.digestSpi = DigestSpi.getInstance(hashAlgorithm);
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
                byte[] publicKeyData = inputStream.read(publicKeyLen);

                // 使用指定数据解析出ECPoint
                ECPoint Q = domainParameters.getCurve().decodePoint(publicKeyData);

                // EC密钥交换算法服务端公钥
                ecAgreeServerPublicKey = new ECPublicKeyParameters(Q, domainParameters);

                // 初始化本地公私钥，用于后续密钥交换
                AsymmetricCipherKeyPair asymmetricCipherKeyPair = generateECKeyPair(
                    domainParameters);
                this.ecAgreeClientPrivateKey = (ECPrivateKeyParameters) asymmetricCipherKeyPair
                    .getPrivate();
                this.ecAgreeClientPublicKey = (ECPublicKeyParameters) asymmetricCipherKeyPair
                    .getPublic();

                // 计算preMasterKey
                this.preMasterKey = calculateECDHBasicAgreement(this.ecAgreeServerPublicKey,
                    this.ecAgreeClientPrivateKey);

                // 这里就先不验签了
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
                signature.update(publicKeyData);

                if (!signature.verify(serverSignData)) {
                    throw new RuntimeException("签名验证失败");
                }

                break;
            case SERVER_HELLO_DONE:
                // 详见README中，有详细算法
                PhashSpi phashSpi = PhashSpi
                    .getInstance(this.serverHello.getCipherSuite().getMacDesc().getMacAlg());
                phashSpi.init(preMasterKey);

                masterSecret = new byte[48];
                byte[] label = "master secret".getBytes();
                byte[] seed = new byte[label.length + clientRandom.length
                                       + serverHello.getServerRandom().length];

                System.arraycopy(label, 0, seed, 0, label.length);
                System.arraycopy(clientRandom, 0, seed, label.length, clientRandom.length);
                System.arraycopy(serverHello.getServerRandom(), 0, seed,
                    label.length + clientRandom.length, serverHello.getServerRandom().length);
                // 计算master_key
                phashSpi.phash(seed, masterSecret);
                System.out.println("生成的masterSecret是：" + Arrays.toString(masterSecret));

                // 初始化cipher
                CipherSuite.CipherDesc cipherDesc = this.serverHello.getCipherSuite().getCipher();
                int macLen = this.serverHello.getCipherSuite().getMacDesc().getMacLen();
                int cipherKeyLen = this.serverHello.getCipherSuite().getCipher().getKeySize();
                int ivLen = this.serverHello.getCipherSuite().getCipher().getIvLen();

                byte[] output = new byte[(macLen + cipherKeyLen + ivLen) * 2];
                phashSpi.init(masterSecret);
                phashSpi.phash(CollectionUtil.merge("key expansion".getBytes(),
                    CollectionUtil.merge(this.serverHello.getServerRandom(), this.clientRandom)),
                    output);
                // TODO

                // 准备发送client key exchange消息
                ECDHClientKeyExchange ecdhClientKeyExchange = new ECDHClientKeyExchange(
                    ecAgreeClientPublicKey);
                write(ecdhClientKeyExchange, outputRecord);
                System.out.println("ECDHClientKeyExchange消息发送完毕");
                sendChangeCipher(outputRecord);
                System.out.println("changeCipherSpec消息发送完毕");
                // TODO 这里应该先更改outputRecord的加密器，然后在发送finish消息

                new Finished(digestSpi, this.serverHello.getCipherSuite().getMacDesc().getMacAlg(),
                    masterSecret, true).write(outputRecord);
                System.out.println("client finish消息发送完毕");
                break;
        }
    }

    private static void write(HandshakeMessage message,
                              WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt8(ContentType.HANDSHAKE.getCode());
        TlsVersion.TLS1_2.write(outputStream);
        outputStream.writeInt16(message.size());
        message.write(outputStream);
        outputStream.flush();
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
        Socket socket = new Socket("39.156.66.14", 443);

        OutputRecord outputStream = new OutputRecord(socket.getOutputStream());

        ClientHello hello = new ClientHello("baidu.com");
        write(hello, outputStream);

        InputRecord inputStream = new InputRecord(socket.getInputStream());
        ClientHandshaker handshaker = new ClientHandshaker();
        handshaker.inputRecord = inputStream;
        handshaker.outputRecord = outputStream;
        handshaker.clientRandom = hello.getClientRandom();

        //        {
        //            System.out.println("当前可用长度：" + inputStream.available());
        //            Thread.sleep(1000);
        //            System.out.println("当前可用长度：" + inputStream.available());
        //            Thread.sleep(1000);
        //            System.out.println("当前可用长度：" + inputStream.available());
        //            Thread.sleep(1000);
        //            System.out.println("当前可用长度：" + inputStream.available());
        //            Thread.sleep(1000 * 5);
        //
        //            byte[] buffer = new byte[inputStream.available()];
        //
        //            inputStream.read(buffer);
        //            System.out.println(Arrays.toString(buffer));
        //            System.exit(1);
        //
        //
        //            int readLen;
        //            while ((readLen = inputStream.read(buffer)) > 0) {
        //                System.out.println(readLen);
        //                System.out.println(Arrays.toString(buffer));
        //            }
        //
        //            System.out.println("退出了");
        //            System.exit(1);
        //        }

        while (true) {
            int contentType = inputStream.read();
            System.out
                .println("contentType:" + EnumInterface.getByCode(contentType, ContentType.class)
                         + " : " + contentType);
            int version = inputStream.readInt16();
            System.out.printf("version: %x%n", version);
            int len = inputStream.readInt16();
            System.out.println("len:" + len);
            System.out.println("可用：" + inputStream.available());
            byte[] data = inputStream.read(len);
            System.out.println("type:" + data[0]);
            handshaker.process(data);
            System.out.println("\n\n");
        }
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
