package com.joe.ssl.example;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.utils.collection.CollectionUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * AES示例
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-05 15:13
 */
public class AesExample {

    public static void main(String[] args) throws Exception {
        // 加密算法
        CipherSuite.CipherDesc algorithm = CipherSuite.CipherDesc.AES_256_GCM;

        SecretKey secretKey;

        {
            // 密钥随机生成
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(algorithm.keySize * 8, new SecureRandom());
            secretKey = keyGen.generateKey();
        }

        {
            // 用指定密钥生成
            byte[] key = new byte[algorithm.keySize];
            // 这里不填充全用0也行，只是演示用，实际上这里是客户端和服务端协商的
            Arrays.fill(key, (byte) 10);
            secretKey = new SecretKeySpec(key, "AES");
        }

        // 模拟iv，如果是GCM模式，实际上这里的iv应该是 fixedIv + nonce，fixedIvLen + nonce = ivLen
        // 这里只是为了演示，所以也只是初始化为0
        byte[] iv = new byte[algorithm.ivLen];

        // 要加密的数据
        String data = "你好啊，这是一段加密测试文本，如果运行正确应该原样返回，随机数据：三六九等费卢卡斯极度分裂加上代理开发机阿杀戮空间发" +
                "拉萨极度分裂加锁离开房间沙龙课到件方收款加的夫拉设计费咯科技爱上了的放款加上了开的房间流口水极度分裂看加上了开的房间阿杀戮空" +
                "间发雷克萨机房了空间撒量放款加两颗到件方拉实际多发咯科技撒肥料空间撒了开的房间老和尚都顾杀了曼妮芬票数与adol侯三双链监控多in" +
                "没离开妈都是GFUI零食都能即佛看偶数难道官方";
        byte[] encryptData = data.getBytes();

        // 使用JDK自带的JCE实现来加密，注意对于jce实现来说，这里是要区分加密模式的
        byte[] jceResult = null;
        if (algorithm.cipherType == CipherSuite.CipherType.AHEAD) {
            // 这里16 * 8 是固定的
            jceResult = doCipher(encryptData, algorithm, Cipher.ENCRYPT_MODE, secretKey, null, new GCMParameterSpec(16 * 8, iv));
        } else if (algorithm.cipherType == CipherSuite.CipherType.BLOCK) {
            jceResult = doCipher(encryptData, algorithm, Cipher.ENCRYPT_MODE, secretKey, null, new IvParameterSpec(iv));
        }

        // 使用BouncyCastle里边的实现来加密
        byte[] bcResult = doCipher(encryptData, algorithm, Cipher.ENCRYPT_MODE, secretKey, new BouncyCastleProvider(), new IvParameterSpec(iv));
        // 对比加密结果是否一致，应该是要一致的
        if (!Arrays.equals(jceResult, bcResult)) {
            throw new RuntimeException("加密结果不一致");
        }

        // 使用BouncyCastle解密数据
        byte[] decryptData = doCipher(bcResult, algorithm, Cipher.DECRYPT_MODE, secretKey, new BouncyCastleProvider(), new IvParameterSpec(iv));
        if (!data.equals(new String(decryptData, 0, encryptData.length))) {
            throw new RuntimeException("解密失败，解密结果跟原数据不一致");
        }
    }

    public static byte[] doCipher(byte[] data, CipherSuite.CipherDesc algorithm, int mode, SecretKey secretKey,
                                  Provider provider, AlgorithmParameterSpec algorithmParameterSpec) throws Exception {
        // 算法实现
        Cipher cipher;

        if (provider == null) {
            cipher = Cipher.getInstance(algorithm.cipherName);
        } else {
            cipher = Cipher.getInstance(algorithm.cipherName, provider);
        }

        // 因为我们的选定的算法都是NoPadding，所以需要自己填充对齐，确定算法后就可以获取块大小然后进行填充了，GCM模式不需要填充
        if (algorithm.cipherType == CipherSuite.CipherType.BLOCK) {
            data = padding(data, cipher.getBlockSize());
        }

        // 对应的iv，GCM模式下实际上这里边的数据应该是 fixedIv + nonce ， 总长度还是等于ivLen，nonce对应的
        // 是TCP的seqNumber（这个值每次都会变），所以实际运用中GCM模式每次都需要重新初始化cipher
        byte[] iv = new byte[algorithm.ivLen];

        IvParameterSpec ivps = new IvParameterSpec(iv);

        // 生成密钥
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(algorithm.keySize * 8, new SecureRandom());

        // 加密模式
        cipher.init(mode, secretKey, algorithmParameterSpec, new SecureRandom());
        return cipher.doFinal(data);
    }

    public static byte[] padding(byte[] src, int blockSize) {
        int paddingLen = blockSize - src.length % blockSize;

        if (paddingLen == blockSize) {
            return src;
        }

        return CollectionUtil.merge(src, new byte[paddingLen]);
    }
}
