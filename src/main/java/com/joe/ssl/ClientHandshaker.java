package com.joe.ssl;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import com.joe.ssl.crypto.AlgorithmRegistry;
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

    /**
     * 加密套件
     */
    private CipherSuiteList        cipherSuiteList;

    /**
     * 处理握手数据
     *
     * @param handshakeData 握手数据
     */
    public void process(byte[] handshakeData) throws Exception {
        HandshakeType type = HandshakeType.getByCode(handshakeData[0]);
        byte[] realData = new byte[handshakeData.length - 4];
        System.arraycopy(handshakeData, 4, realData, 0, realData.length);
        System.out.println("收到\"" + type + "\"类型的握手数据");
        switch (type) {
            case SERVER_HELLO:
                this.serverHello = new ServerHello(handshakeData);
                break;
            case CERTIFICATE:
                // 这里先不管证书，采用ECC相关算法时证书只用来签名
                break;
            case SERVER_KEY_EXCHANGE:
                // 处理服务端的密钥交换
                int curveType = realData[0];
                // 这个必须等于3，其他不处理，目前应该也不会有其他的值
                Assert.isTrue(curveType == 3);
                int curveId = realData[1] << 8 | realData[2];

                ECDomainParameters domainParameters = NamedCurve.getECParameters(curveId);

                // 如果等于null表示不支持
                if (domainParameters == null) {
                    throw new RuntimeException(String.format("不支持的椭圆曲线id：%d", curveId));
                }

                int publicKeyLen = realData[3];
                byte[] publicKeyData = new byte[publicKeyLen];

                System.arraycopy(realData, 4, publicKeyData, 0, publicKeyLen);

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
                break;
            case SERVER_HELLO_DONE:
                // 详见README中，有详细算法
                PhashSpi phashSpi = AlgorithmRegistry
                    .newInstance("Phash" + this.serverHello.getCipherSuite().getMacAlg());
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
                break;
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println(AlgorithmRegistry.getAllAlgorithm());

        // ip.src == 39.156.66.14 || ip.dst == 39.156.66.14
        Socket socket = new Socket("39.156.66.14", 443);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        WrapedOutputStream wrapedOutputStream = new WrapedOutputStream(socket.getOutputStream());
        //        WrapedOutputStream wrapedOutputStream = new WrapedOutputStream(outputStream);

        ClientHello hello = new ClientHello("baidu.com");
        wrapedOutputStream.writeInt8(ContentType.HANDSHAKE.getCode());
        wrapedOutputStream.writeInt8(TlsVersion.TLS1_2.getMajorVersion());
        wrapedOutputStream.writeInt8(TlsVersion.TLS1_2.getMinorVersion());
        wrapedOutputStream.writeInt16(hello.size());
        hello.write(wrapedOutputStream);
        wrapedOutputStream.flush();

        byte[] helloData = outputStream.toByteArray();
        socket.getOutputStream().write(helloData);

        //        System.out.println(hello);
        //        System.out.println("\n\n");
        //        System.out.println(Arrays.toString(helloData));
        //        System.out.println("\n\n");
        //        hello = new ClientHello(Arrays.copyOfRange(helloData, 5, helloData.length));
        //        System.out.println(hello);

        //        Thread.sleep(1000);
        //        System.exit(1);

        WrapedInputStream inputStream = new WrapedInputStream(socket.getInputStream());
        ClientHandshaker handshaker = new ClientHandshaker();
        handshaker.clientRandom = hello.getClientRandom();

        while (true) {
            int contentType = inputStream.read();
            System.out
                .println("contentType:" + EnumInterface.getByCode(contentType, ContentType.class));
            int version = inputStream.readInt16();
            System.out.println(String.format("version: %x", version));
            int len = inputStream.readInt16();
            System.out.println("len:" + len);
            byte[] data = inputStream.read(len);

            handshaker.process(data);
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
