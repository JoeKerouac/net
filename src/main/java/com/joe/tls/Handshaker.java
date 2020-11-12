package com.joe.tls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.X509KeyManager;
import javax.swing.text.AbstractDocument;

import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.crypto.ECDHKeyExchangeSpi;
import com.joe.tls.crypto.PhashSpi;
import com.joe.tls.crypto.impl.BCECDHKeyExchangeSpi;
import com.joe.tls.enums.ContentType;
import com.joe.tls.enums.NamedCurve;
import com.joe.tls.impl.InputRecordStreamImpl;
import com.joe.tls.impl.OutputRecordStreamImpl;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.Record;
import com.joe.tls.msg.extensions.ExtensionType;
import com.joe.tls.msg.extensions.HelloExtension;
import com.joe.tls.msg.extensions.RenegotiationInfoExtension;
import com.joe.tls.msg.impl.*;
import com.joe.utils.collection.CollectionUtil;

/**
 * @author JoeKerouac
 * @data 2020-11-06 22:34
 */
public class Handshaker {

    protected HandshakeHash      handshakeHash;

    protected SecretCollection   secretCollection;

    protected InputRecordStream  inputRecordStream;

    protected OutputRecordStream outputRecordStream;

    private String               serverName;

    /**
     * 密钥管理
     */
    private X509KeyManager       keyManager;

    /**
     * 安全随机数
     */
    protected SecureRandom       secureRandom;

    /**
     * 最终选择的加密套件
     */
    protected CipherSuite        cipherSuite;

    /**
     * phash算法
     */
    private PhashSpi             phashSpi;

    /**
     * 服务端证书公钥
     */
    private PublicKey            publicKey;

    /**
     * 客户端随机数
     */
    private byte[]               clientRandom;

    /**
     * 服务端随机数
     */
    private byte[]               serverRandom;

    /**
     * 客户端密钥交换使用密钥对
     */
    private ECDHKeyPair          ecdhKeyPair;

    /**
     * 密钥交换SPI
     */
    private ECDHKeyExchangeSpi   keyExchangeSpi;

    /**
     * TLS版本号
     */
    protected TlsVersion         tlsVersion;

    /**
     * 如果serverHello中包含EXT_EXTENDED_MASTER_SECRET扩展则设置为true；
     */
    private boolean              extExtendedMasterSecret = false;

    /**
     * 当前是否是client
     */
    private final boolean        isClient;

    /**
     * 证书私钥，服务端有
     */
    private PrivateKey           privateKey;

    /**
     * 证书链，如果是服务端有
     */
    private Certificate[]        certChain;

    public Handshaker(InputStream netInputStream, OutputStream netOutputStream,
                      SecureRandom secureRandom, boolean isClient) {
        this.handshakeHash = new HandshakeHash();
        this.secretCollection = new SecretCollection();
        this.secureRandom = secureRandom;
        this.inputRecordStream = new InputRecordStreamImpl(netInputStream, handshakeHash,
            TlsVersion.TLS1_2);
        this.outputRecordStream = new OutputRecordStreamImpl(netOutputStream, handshakeHash,
            TlsVersion.TLS1_2);
        this.isClient = isClient;
    }

    /**
     * 启动握手，将会同步等待握手完成
     * @throws IOException IO异常
     */
    public void kickstart() throws IOException {
        // 如果是客户端，需要先发送clientHello消息
        if (isClient) {
            ClientHello clientHello = new ClientHello(serverName, secureRandom);
            this.clientRandom = clientHello.getClientRandom();
            Record clientHelloRecord = new Record(ContentType.HANDSHAKE, TlsVersion.TLS1_2,
                clientHello);
            outputRecordStream.write(clientHelloRecord);
        }

        byte[] sessionHash;
        ClientHello clientHello;
        ServerHello serverHello;
        CipherSuite cipherSuite;
        CertificateMsg certificateMsg;
        ECDHServerKeyExchange ecdhServerKeyExchange;
        ServerHelloDone serverHelloDone;

        while (true) {
            Record readRecord = inputRecordStream.read();
            switch (readRecord.type()) {
                case HANDSHAKE:
                    HandshakeProtocol protocol = (HandshakeProtocol) readRecord.msg();
                    switch (protocol.type()) {
                        case CLIENT_HELLO:
                            clientHello = (ClientHello) protocol;

                            extExtendedMasterSecret = clientHello
                                .getExtension(ExtensionType.EXT_EXTENDED_MASTER_SECRET) != null;

                            // 客户端发过来的算法套件与服务端支持的算法套件的交集
                            List<CipherSuite> mixed = new ArrayList<>();
                            List<CipherSuite> supports = CipherSuite.getAllSupports();
                            for (CipherSuite suite : clientHello.getCipherSuites()) {
                                if (supports.contains(suite)) {
                                    mixed.add(suite);
                                }
                            }

                            cipherSuite = chooseCipherSuite(mixed);

                            if (cipherSuite == null) {
                                throw new RuntimeException(
                                    String.format("当前服务端支持的加密套件列表：%s，客户端支持的加密套件列表：%s，没有共同支持的加密套件",
                                        supports, clientHello.getCipherSuites()));
                            }

                            // 初始化加密套件
                            initCipherSuite(cipherSuite);
                            this.serverRandom = new byte[32];
                            secureRandom.nextBytes(serverRandom);
                            this.tlsVersion = TlsVersion.TLS1_2;

                            List<HelloExtension> extensions = new ArrayList<>();

                            HelloExtension serverName = clientHello
                                .getExtension(ExtensionType.EXT_SERVER_NAME);
                            if (serverName != null) {
                                extensions.add(serverName);
                            }
                            extensions.add(new RenegotiationInfoExtension());
                            serverHello = new ServerHello(tlsVersion, serverRandom, new byte[0],
                                cipherSuite, extensions);

                            // 写出server hello消息
                            outputRecordStream
                                .write(new Record(ContentType.HANDSHAKE, tlsVersion, serverHello));

                            certificateMsg = new CertificateMsg(certChain);
                            outputRecordStream.write(new Record(ContentType.HANDSHAKE, tlsVersion, certificateMsg));

                            int curveId = NamedCurve.getAllSupportCurve().get(0).getId();
                            ecdhKeyPair = keyExchangeSpi.generate(curveId);

                            // 这里选择合适的签名算法对公钥签名
//                            ecdhServerKeyExchange = new ECDHServerKeyExchange(3, curveId, ecdhKeyPair.getPublicKey(), cipherSuite.getHashDesc())
                            break;
                        case SERVER_HELLO:
                            serverHello = (ServerHello) protocol;
                            tlsVersion = serverHello.getVersion();

                            if (tlsVersion != TlsVersion.TLS1_2) {
                                throw new RuntimeException("不支持的版本号：" + tlsVersion);
                            }

                            serverRandom = serverHello.getServerRandom();
                            cipherSuite = serverHello.getCipherSuite();
                            initCipherSuite(cipherSuite);

                            // 判断是否包含EXT_EXTENDED_MASTER_SECRET这个扩展，如果包含masterKey计算方式将会改变
                            if (serverHello
                                .getExtension(ExtensionType.EXT_EXTENDED_MASTER_SECRET) != null) {
                                extExtendedMasterSecret = true;
                            }

                            break;
                        case CERTIFICATE:
                            certificateMsg = (CertificateMsg) protocol;
                            publicKey = certificateMsg.getPublicKey();
                            break;
                        case SERVER_KEY_EXCHANGE:
                            ecdhServerKeyExchange = (ECDHServerKeyExchange) protocol;
                            ecdhKeyPair = keyExchangeSpi
                                .generate(ecdhServerKeyExchange.getCurveId());
                            byte[] preMasterKey = keyExchangeSpi.keyExchange(
                                ecdhServerKeyExchange.getPublicKey(), ecdhKeyPair.getPrivateKey(),
                                ecdhServerKeyExchange.getCurveId());
                            secretCollection.setPreMasterKey(preMasterKey);
                            Signature signature = SignatureAndHashAlgorithm
                                .newSignatureAndHash(ecdhServerKeyExchange.getHashAndSigAlg());
                            try {
                                signature.initVerify(publicKey);
                                signature.update(clientRandom);
                                signature.update(serverRandom);
                                signature.update(ecdhServerKeyExchange.getCurveType());
                                signature.update((byte) (ecdhServerKeyExchange.getCurveId() >> 8));
                                signature.update((byte) ecdhServerKeyExchange.getCurveId());
                                signature
                                    .update((byte) ecdhServerKeyExchange.getPublicKey().length);
                                signature.update(ecdhServerKeyExchange.getPublicKey());
                                if (!signature.verify(ecdhServerKeyExchange.getSig())) {
                                    throw new RuntimeException("签名验证失败");
                                }
                            } catch (InvalidKeyException | SignatureException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case SERVER_HELLO_DONE:
                            // 发送客户端密钥交换消息
                            ECDHClientKeyExchange clientKeyExchange = new ECDHClientKeyExchange(
                                ecdhKeyPair.getPublicKey());
                            outputRecordStream.write(
                                new Record(ContentType.HANDSHAKE, tlsVersion, clientKeyExchange));

                            // 发送changeCipherSpace消息
                            ChangeCipherSpace changeCipherSpace = new ChangeCipherSpace();
                            outputRecordStream.write(new Record(ContentType.CHANGE_CIPHER_SPEC,
                                tlsVersion, changeCipherSpace));

                            sessionHash = handshakeHash.getFinishedHash();
                            secretCollection.setSessionHash(sessionHash);

                            // 更改读写流的加密器
                            changeCipher();

                            // 写出finish消息
                            Finished finished = new Finished(phashSpi,
                                secretCollection.getMasterKey(), sessionHash, isClient);

                            outputRecordStream
                                .write(new Record(ContentType.HANDSHAKE, tlsVersion, finished));
                            break;
                        case FINISHED:
                            if (isClient) {
                                // client收到这个消息说明已经握手完成，这里就不校验服务端的finished消息是否正确了
                                return;
                            } else {
                                // 服务端需要校验客户端的finished数据
                                // 客户端实际的finished消息
                                Finished remoteFinished = (Finished) protocol;
                                sessionHash = handshakeHash.getFinishedHash();
                                // 本地计算的客户端finished消息
                                Finished clientFinished = new Finished(phashSpi,
                                    secretCollection.getMasterKey(), sessionHash, true);

                                // 对比本地计算的clientFinished数据和实际client发过来的finished数据是否一致，不一致说明有问题
                                if (!Arrays.equals(clientFinished.getData(),
                                    remoteFinished.getData())) {
                                    throw new RuntimeException(
                                        "本地计算finished消息和client发送的finished消息不一致");
                                }

                                // finished消息没问题，写出更改密钥空间消息
                                outputRecordStream.write(new Record(ContentType.CHANGE_CIPHER_SPEC,
                                    tlsVersion, new ChangeCipherSpace()));

                                Finished serverFinished = new Finished(phashSpi,
                                    secretCollection.getMasterKey(), sessionHash, false);
                                // 写出服务端的finished消息
                                outputRecordStream.write(
                                    new Record(ContentType.HANDSHAKE, tlsVersion, serverFinished));
                            }
                    }

                    break;
                case CHANGE_CIPHER_SPEC:
                    ChangeCipherSpace changeCipherSpace = (ChangeCipherSpace) readRecord.msg();
                    // 服务端收到客户端的changeCipherSpace就开始更改cipher
                    if (!isClient) {
                        // 开始计算masterKey、连接相关key、changeCipher
                        changeCipher();
                    }
                    break;
                case APPLICATION_DATA:
                case ALTER:
                default:
                    throw new RuntimeException("暂时不支持的类型：" + readRecord.type());
            }
        }
    }

    private CipherSuite chooseCipherSuite(List<CipherSuite> cipherSuites) {
        for (CipherSuite suite : cipherSuites) {
            switch (suite.getKeyExchange()) {
                case ECDHE_RSA:
                    if (setupPrivateKeyAndChain("RSA")) {
                        return suite;
                    }
                    break;
                default:
                    throw new RuntimeException("不支持的密钥交换算法：" + suite.getKeyExchange());
            }
        }

        return null;
    }

    private boolean setupPrivateKeyAndChain(String algorithm) {
        String alias = keyManager.chooseServerAlias(algorithm, null, null);
        // 如果别名为null，说明当前没有支持该算法的证书
        if (alias == null) {
            return false;
        }

        // 查找指定别名的私钥
        PrivateKey privateKey = keyManager.getPrivateKey(alias);
        if (privateKey == null) {
            return false;
        }

        // 查找该别名的证书链
        X509Certificate[] certChain = keyManager.getCertificateChain(alias);
        if (certChain == null || certChain.length == 0) {
            return false;
        }

        // 确保公私钥的算法符合指定算法
        String keyAlg = algorithm.split("_")[0];
        PublicKey publicKey = certChain[0].getPublicKey();
        if (!privateKey.getAlgorithm().equals(keyAlg) || !publicKey.getAlgorithm().equals(keyAlg)) {
            return false;
        }

        this.privateKey = privateKey;
        this.certChain = certChain;
        return true;
    }

    /**
     * 初始化加密套件
     * @param cipherSuite 选择的加密套件
     */
    private void initCipherSuite(CipherSuite cipherSuite) {
        this.cipherSuite = cipherSuite;
        String hashAlg = cipherSuite.getHashDesc().getHashAlg();
        handshakeHash.setFinishedAlg(hashAlg);
        phashSpi = PhashSpi.getInstance(hashAlg);
        this.keyExchangeSpi = new BCECDHKeyExchangeSpi();
    }

    /**
     * 计算masterKey
     */
    private void calcMaster() {
        byte[] sessionHash = secretCollection.getSessionHash();
        byte[] preMasterKey = secretCollection.getPreMasterKey();
        byte[] label;
        byte[] seed;

        if (extExtendedMasterSecret) {
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
        secretCollection.setMasterKey(masterSecret);
    }

    /**
     * 计算连接相关的key
     */
    private void calcConnectionKeys() {
        phashSpi.init(secretCollection.getMasterKey());
        int macLen = cipherSuite.getMacDesc().getMacLen();
        int cipherKeyLen = cipherSuite.getCipher().getKeySize();
        int ivLen = cipherSuite.getCipher().getFixedIvLen();
        ivLen = ivLen == 0 ? cipherSuite.getCipher().getIvLen() : ivLen;
        byte[] output = new byte[(macLen + cipherKeyLen + ivLen) * 2];
        phashSpi.phash(CollectionUtil.merge("key expansion".getBytes(),
            CollectionUtil.merge(serverRandom, clientRandom)), output);

        if (macLen > 0) {
            byte[] clientWriteMac = new byte[macLen];
            byte[] serverWriteMac = new byte[macLen];
            System.arraycopy(output, 0, clientWriteMac, 0, macLen);
            System.arraycopy(output, macLen, serverWriteMac, 0, macLen);
            secretCollection.setClientMac(clientWriteMac);
            secretCollection.setServerMac(serverWriteMac);
        }

        if (cipherKeyLen > 0) {
            byte[] clientCipherKey = new byte[cipherKeyLen];
            byte[] serverCipherKey = new byte[cipherKeyLen];
            System.arraycopy(output, 2 * macLen, clientCipherKey, 0, cipherKeyLen);
            System.arraycopy(output, 2 * macLen + cipherKeyLen, serverCipherKey, 0, cipherKeyLen);
            secretCollection.setClientWriteKey(clientCipherKey);
            secretCollection.setServerWriteKey(serverCipherKey);
        }

        if (ivLen > 0) {
            byte[] clientIv = new byte[ivLen];
            byte[] serverIv = new byte[ivLen];
            System.arraycopy(output, 2 * macLen + 2 * cipherKeyLen, clientIv, 0, ivLen);
            System.arraycopy(output, 2 * macLen + 2 * cipherKeyLen + ivLen, serverIv, 0, ivLen);
            secretCollection.setClientWriteIv(clientIv);
            secretCollection.setServerWriteIv(serverIv);
        }
    }

    /**
     * 更改传输流的加密器
     */
    protected void changeCipher() {
        // 先计算相关密钥
        calcMaster();
        calcConnectionKeys();

        // 客户端和服务端使用的密钥不一致，这里做区分
        byte[] writeKey = isClient ? secretCollection.getClientWriteKey()
            : secretCollection.getServerWriteKey();
        byte[] writeIv = isClient ? secretCollection.getClientWriteIv()
            : secretCollection.getServerWriteIv();
        byte[] readKey = isClient ? secretCollection.getServerWriteKey()
            : secretCollection.getClientWriteKey();
        byte[] readIv = isClient ? secretCollection.getServerWriteIv()
            : secretCollection.getClientWriteIv();

        // 客户端写出加密盒
        CipherBox writeCipherBox = new CipherBox(secureRandom, cipherSuite.getCipher(), writeKey,
            writeIv, true);
        // 服务端读取加密盒
        CipherBox readCipherBox = new CipherBox(secureRandom, cipherSuite.getCipher(), readKey,
            readIv, false);

        if (cipherSuite.getCipher().isGcm()) {
            Authenticator readAuthenticator = new Authenticator(tlsVersion);
            Authenticator writeAuthenticator = new Authenticator(tlsVersion);
            inputRecordStream.changeCipher(readCipherBox, readAuthenticator);
            outputRecordStream.changeCipher(writeCipherBox, writeAuthenticator);
        } else {
            throw new RuntimeException("暂不支持的算法：" + cipherSuite.getCipher());
        }
    }

}
