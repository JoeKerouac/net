package com.joe.tls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.ssl.crypto.PhashSpi;
import com.joe.ssl.crypto.impl.BCECDHKeyExchangeSpi;
import com.joe.ssl.example.ECDHKeyPair;
import com.joe.ssl.message.TlsVersion;
import com.joe.tls.enums.ContentType;
import com.joe.tls.impl.InputRecordStreamImpl;
import com.joe.tls.impl.OutputRecordStreamImpl;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.Record;
import com.joe.tls.msg.extensions.ExtensionType;
import com.joe.tls.msg.impl.*;
import com.joe.utils.collection.CollectionUtil;

/**
 * @author JoeKerouac
 * @data 2020-11-06 22:34
 */
public abstract class Handshaker {

    protected HandshakeHash      handshakeHash;

    protected SecretCollection   secretCollection;

    protected InputRecordStream  inputRecordStream;

    protected OutputRecordStream outputRecordStream;

    private String               serverName;

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

    public Handshaker(InputStream netInputStream, OutputStream netOutputStream,
                      SecureRandom secureRandom) {
        this.handshakeHash = new HandshakeHash();
        this.secretCollection = new SecretCollection();
        this.secureRandom = secureRandom;
        this.inputRecordStream = new InputRecordStreamImpl(netInputStream, handshakeHash,
            TlsVersion.TLS1_2);
        this.outputRecordStream = new OutputRecordStreamImpl(netOutputStream, handshakeHash,
            TlsVersion.TLS1_2);
        isClient = this instanceof ClientHandshaker;
    }

    /**
     * 启动握手，将会同步等待握手完成
     * @throws IOException IO异常
     */
    public void kickstart() throws IOException {
        ClientHello clientHello = new ClientHello(serverName, secureRandom);
        this.clientRandom = clientHello.getClientRandom();
        Record clientHelloRecord = new Record(ContentType.HANDSHAKE, TlsVersion.TLS1_2,
            clientHello);
        outputRecordStream.write(clientHelloRecord);

        while (true) {
            Record readRecord = inputRecordStream.read();
            switch (readRecord.type()) {
                case HANDSHAKE:
                    HandshakeProtocol protocol = (HandshakeProtocol) readRecord.msg();
                    switch (protocol.type()) {
                        case SERVER_HELLO:
                            ServerHello serverHello = (ServerHello) protocol;
                            tlsVersion = serverHello.getVersion();

                            if (tlsVersion != TlsVersion.TLS1_2) {
                                throw new RuntimeException("不支持的版本号：" + tlsVersion);
                            }

                            serverRandom = serverHello.getServerRandom();
                            CipherSuite cipherSuite = serverHello.getCipherSuite();
                            initCipherSuite(cipherSuite);

                            // 判断是否包含EXT_EXTENDED_MASTER_SECRET这个扩展，如果包含masterKey计算方式将会改变
                            if (serverHello
                                .getExtension(ExtensionType.EXT_EXTENDED_MASTER_SECRET) != null) {
                                extExtendedMasterSecret = true;
                            }

                            break;
                        case CERTIFICATE:
                            CertificateMsg certificateMsg = (CertificateMsg) protocol;
                            publicKey = certificateMsg.getPublicKey();
                            break;
                        case SERVER_KEY_EXCHANGE:
                            ECDHServerKeyExchange keyExchange = (ECDHServerKeyExchange) protocol;
                            ecdhKeyPair = keyExchangeSpi.generate(keyExchange.getCurveId());
                            byte[] preMasterKey = keyExchangeSpi.keyExchange(
                                keyExchange.getPublicKey(), ecdhKeyPair.getPrivateKey(),
                                keyExchange.getCurveId());
                            secretCollection.setPreMasterKey(preMasterKey);
                            Signature signature = SignatureAndHashAlgorithm
                                .newSignatureAndHash(keyExchange.getHashAndSigAlg());
                            try {
                                signature.initVerify(publicKey);
                                signature.update(clientRandom);
                                signature.update(serverRandom);
                                signature.update(keyExchange.getCurveType());
                                signature.update((byte) (keyExchange.getCurveId() >> 8));
                                signature.update((byte) keyExchange.getCurveId());
                                signature.update((byte) keyExchange.getPublicKey().length);
                                signature.update(keyExchange.getPublicKey());
                                if (!signature.verify(keyExchange.getSig())) {
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

                            byte[] sessionHash = handshakeHash.getFinishedHash();
                            secretCollection.setSessionHash(sessionHash);

                            calcMaster();

                            // 算完masterKey开始算连接key，连接key依赖于masterKey
                            calcConnectionKeys();

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
                                // client收到这个消息说明已经握手完成
                                return;
                            } else {
                                throw new RuntimeException("当前还不支持Server端收到finished消息");
                            }
                    }

                    break;
                case CHANGE_CIPHER_SPEC:
                    ChangeCipherSpace changeCipherSpace = (ChangeCipherSpace) readRecord.msg();
                    break;
                case APPLICATION_DATA:
                case ALTER:
                default:
                    throw new RuntimeException("暂时不支持的类型：" + readRecord.type());
            }
        }
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
    protected abstract void changeCipher();

}
