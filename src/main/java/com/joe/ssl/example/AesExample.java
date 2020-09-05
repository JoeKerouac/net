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
import java.util.Set;

/**
 * AES示例
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-05 15:13
 */
public class AesExample {

    public static void main(String[] args) throws Exception {
//        Set<Provider.Service>  services = new BouncyCastleProvider().getServices();
//        services.forEach(service -> {
//
//            if (service.getAlgorithm().contains("AES") || service.getAlgorithm().contains("aes")) {
//                if (service.getType().equals("Cipher")) {
//                    System.out.println(service.getAlgorithm());
//                    System.out.println(service.getProvider());
//                    System.out.println(service.getType());
//                    System.out.println(service.getClassName());
//                    System.out.println("\n\n");
//                }
//            }
//        });


        String data = "你好啊，这是一段加密测试文本，如果运行正确应该原样返回，随机数据：三六九等费卢卡斯极度分裂加上代理开发机阿杀戮空间发" +
                "拉萨极度分裂加锁离开房间沙龙课到件方收款加的夫拉设计费咯科技爱上了的放款加上了开的房间流口水极度分裂看加上了开的房间阿杀戮空" +
                "间发雷克萨机房了空间撒量放款加两颗到件方拉实际多发咯科技撒肥料空间撒了开的房间老和尚都顾杀了曼妮芬票数与adol侯三双链监控多in" +
                "没离开妈都是GFUI零食都能即佛看偶数难道官方";

        if (!test2(data).equals(data)) {
            throw new RuntimeException("加解密失败，结果对别不一致");
        }


    }

    /**
     * 普通（非GCM）模式加密用例
     * @param encryptData 要加密的数据
     * @return 先加密，然后解密，最终把解密结果返回
     * @throws Exception 异常
     */
    public static String test1(String encryptData) throws Exception {
        // 要加密的数据
        byte[] data = encryptData.getBytes();
        int dataLen = data.length;


        // 算法
        CipherSuite.CipherDesc algorithm = CipherSuite.CipherDesc.AES_256;
        // 算法实现
        Cipher cipher = Cipher.getInstance(algorithm.cipherName, new BouncyCastleProvider());
        // 因为我们的选定的算法都是NoPadding，所以需要自己填充对齐，确定算法后就可以获取块大小然后进行填充了，GCM模式不需要填充
        data = padding(encryptData.getBytes(), cipher.getBlockSize());


        // 对应的iv
        byte[] iv = new byte[algorithm.ivLen];
        IvParameterSpec ivps = new IvParameterSpec(iv);

        // 密钥，注意密钥长度要符合规范
        byte[] key = "1234567890abcdef1234567890abcdef".getBytes();
        // 对应的密钥空间
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // 生成密钥
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(algorithm.keySize * 8, new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();

        // 加密模式
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivps, new SecureRandom());
        byte[] encrypt = cipher.doFinal(data);


        // 解密模式
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivps);
        return new String(cipher.doFinal(encrypt), 0, dataLen);
    }


    /**
     * GCM模式加密用例
     * @param encryptData 要加密的数据
     * @return 先加密，然后解密，最终把解密结果返回
     * @throws Exception 异常
     */
    public static String test2(String encryptData) throws Exception {
        // 要加密的数据
        byte[] data = encryptData.getBytes();
        int dataLen = data.length;


        // 算法
        CipherSuite.CipherDesc algorithm = CipherSuite.CipherDesc.AES_256_GCM;
        // 算法实现
        Cipher cipher = Cipher.getInstance(algorithm.cipherName, new BouncyCastleProvider());

        // 对应的iv，实际上这里边的数据应该是 fixedIv + nonce ， 总长度等于ivLen，nonce对应的是TCP的seqNumber（这个值每次都会变）
        byte[] iv = new byte[algorithm.ivLen];

        // GCM有一个tagSize的概念，表示身份验证的长度，目前已知的都是128bit，即16 byte
        int tagSize = 16;
        GCMParameterSpec spec = new GCMParameterSpec(tagSize * 8, iv);

        // 密钥
        byte[] key = "1234567890abcdef1234567890abcdef".getBytes();
        // 对应的密钥空间
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // 生成密钥
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(algorithm.keySize * 8, new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();

        // 加密模式
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec, new SecureRandom());
        byte[] encrypt = cipher.doFinal(data);


        // 解密模式
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
        return new String(cipher.doFinal(encrypt), 0, dataLen);
    }

    public static byte[] padding(byte[] src, int blockSize) {
        int paddingLen = blockSize - src.length % blockSize;

        if (paddingLen == blockSize) {
            return src;
        }

        return CollectionUtil.merge(src, new byte[paddingLen]);
    }
}
