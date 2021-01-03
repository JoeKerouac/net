package com.joe.tls.example;

import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.joe.tls.cipher.CipherSuite;
import com.joe.utils.collection.CollectionUtil;
import com.sun.crypto.provider.SunJCE;

/**
 * AES示例
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-05 15:13
 */
public class AesExample {

    public static void main(String[] args) throws Exception {
        test1();
        test2();
    }

    /**
     * 加解密测试，使用随机生成的密钥
     *
     * @throws Exception
     *             Exception
     */
    private static void test1() throws Exception {
        // 测试所有加密可用
        for (CipherSuite.CipherDesc value : CipherSuite.CipherDesc.values()) {
            SecretKey secretKey;

            {
                // 密钥随机生成
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(value.getKeySize() * 8, new SecureRandom());
                secretKey = keyGen.generateKey();
            }

            baseTest(value, secretKey, false);

            try {
                // 更改结果，理论上GCM模式的是要抛出异常的，因为GCM自带认证信息，修改后可以检测出来
                baseTest(value, secretKey, true);
            } catch (Exception e) {
                handleException(value, e);
            }
        }
    }

    /**
     * 加解密测试，使用固定密钥
     *
     * @throws Exception
     *             Exception
     */
    private static void test2() throws Exception {
        // 测试所有加密可用
        for (CipherSuite.CipherDesc value : CipherSuite.CipherDesc.values()) {

            SecretKey secretKey;
            {
                // 用指定密钥生成，对应秘钥已经交换完成，使用指定秘钥完成加密
                byte[] key = new byte[value.getKeySize()];
                // 这里不填充全用0也行，只是演示用，实际上这里是客户端和服务端协商的
                Arrays.fill(key, (byte)10);
                secretKey = new SecretKeySpec(key, "AES");
            }

            // 普通测试，理论上要通过的，仅仅是对别BC的加密结果和JCE的加密结果是否一致以及解密结果是否正确
            baseTest(value, secretKey, false);

            try {
                // 更改结果，理论上GCM模式的是要抛出异常的，因为GCM自带认证信息，修改后可以检测出来
                baseTest(value, secretKey, true);
            } catch (Exception e) {
                handleException(value, e);
            }
        }
    }

    /**
     * 处理预期中的异常
     *
     * @param value
     *            当前加密算法
     * @param e
     *            异常
     */
    private static void handleException(CipherSuite.CipherDesc value, Exception e) {
        // GCM模式下，因为自带认证，所以解密的时候直接抛出了BadPaddingException异常，而普通模式下因为没有认证信息，所以解密的时候
        // 并不会对信息进行校验，可以解密成功，但是解密出来的结果跟源数据肯定是对不上的
        if (value.getCipherType() == CipherSuite.CipherType.AEAD) {
            if (!(e instanceof BadPaddingException) || !e.getMessage().equals("mac check in GCM failed")) {
                throw new RuntimeException("预期不符合");
            }
        } else if (value.getCipherType() == CipherSuite.CipherType.BLOCK) {
            if (!(e instanceof RuntimeException) || !e.getMessage().equals("解密失败，解密结果跟原数据不一致")) {
                throw new RuntimeException("预期不符合");
            }
        }
    }

    /**
     * 基础测试
     *
     * @param algorithm
     *            算法
     * @param secretKey
     *            密钥
     * @param change
     *            是否更改加密结果，主要为了验证AEAD模式（GCM）的认证信息是否生效
     * @throws Exception
     *             Exception
     */
    private static void baseTest(CipherSuite.CipherDesc algorithm, SecretKey secretKey, boolean change)
        throws Exception {
        // 模拟iv，如果是GCM模式，实际上这里的iv应该是 fixedIv + nonce，fixedIvLen + nonce = ivLen
        // 这里只是为了演示，所以也只是初始化为0
        byte[] iv = new byte[algorithm.getIvLen()];

        // 要加密的数据
        String data = "你好啊，这是一段加密测试文本，如果运行正确应该原样返回，随机数据：三六九等费卢卡斯极度分裂加上代理开发机阿杀戮空间发"
            + "拉萨极度分裂加锁离开房间沙龙课到件方收款加的夫拉设计费咯科技爱上了的放款加上了开的房间流口水极度分裂看加上了开的房间阿杀戮空"
            + "间发雷克萨机房了空间撒量放款加两颗到件方拉实际多发咯科技撒肥料空间撒了开的房间老和尚都顾杀了曼妮芬票数与adol侯三双链监控多in" + "没离开妈都是GFUI零食都能即佛看偶数难道官方";
        byte[] encryptData = data.getBytes();

        // 使用JDK自带的JCE实现来加密，注意对于jce实现来说，这里是要区分加密模式的
        byte[] jceResult = null;
        if (algorithm.getCipherType() == CipherSuite.CipherType.AEAD) {
            // 这里16 * 8 是固定的，实际上加解密时也不会用到
            jceResult = doCipher(encryptData, algorithm, Cipher.ENCRYPT_MODE, secretKey, new SunJCE(),
                new GCMParameterSpec(16 * 8, iv));
        } else if (algorithm.getCipherType() == CipherSuite.CipherType.BLOCK) {
            jceResult =
                doCipher(encryptData, algorithm, Cipher.ENCRYPT_MODE, secretKey, new SunJCE(), new IvParameterSpec(iv));
        }

        // 使用BouncyCastle里边的实现来加密
        byte[] bcResult = doCipher(encryptData, algorithm, Cipher.ENCRYPT_MODE, secretKey, new BouncyCastleProvider(),
            new IvParameterSpec(iv));
        // 对比加密结果是否一致，应该是要一致的
        if (!Arrays.equals(jceResult, bcResult)) {
            throw new RuntimeException("加密结果不一致");
        }

        // 改变加密结果
        if (change) {
            bcResult[bcResult.length - 1] += 1;
        }

        // 使用BouncyCastle解密数据
        byte[] decryptData = doCipher(bcResult, algorithm, Cipher.DECRYPT_MODE, secretKey, new BouncyCastleProvider(),
            new IvParameterSpec(iv));

        if (!data.equals(new String(decryptData, 0, encryptData.length))) {
            throw new RuntimeException("解密失败，解密结果跟原数据不一致");
        }
    }

    /**
     * 进行加解密处理
     *
     * @param data
     *            源数据
     * @param algorithm
     *            算法
     * @param mode
     *            加密或者解密模式
     * @param secretKey
     *            密钥
     * @param provider
     *            提供器，为空时使用JCE实现
     * @param algorithmParameterSpec
     *            IV，BC模式下无论什么模式的AES都是IvParameterSpec，而JCE模式下如果是AEAD模式的AES需要传入GCMParameterSpec
     * @return 加/解密结果
     * @throws Exception
     *             Exception
     */
    private static byte[] doCipher(byte[] data, CipherSuite.CipherDesc algorithm, int mode, SecretKey secretKey,
        Provider provider, AlgorithmParameterSpec algorithmParameterSpec) throws Exception {
        // 算法实现
        Cipher cipher;

        if (provider == null) {
            cipher = Cipher.getInstance(algorithm.getCipherName());
        } else {
            cipher = Cipher.getInstance(algorithm.getCipherName(), provider);
        }

        // 因为我们的选定的算法都是NoPadding，所以需要自己填充对齐，确定算法后就可以获取块大小然后进行填充了，GCM模式不需要填充
        if (algorithm.getCipherType() == CipherSuite.CipherType.BLOCK) {
            // 实际上这里的blockSize固定一直都是16
            data = padding(data, cipher.getBlockSize());
        }

        // 加密模式
        cipher.init(mode, secretKey, algorithmParameterSpec, new SecureRandom());
        return cipher.doFinal(data);
    }

    /**
     * 填充算法：如果源数据长度是6，blockSize是8，那么可以选择填充1，也可以填充9，也可以是17，只要填充后长度是8的整数倍即可，同时填充最大只能到254
     * <p>
     * 注：为什么数据长度6，blockSize是8，只需要填充1？因为最后还会填充一个byte的长度信息，表示填充了多少byte的数据
     *
     * @param src
     *            源数据
     * @param blockSize
     *            块大小
     * @return 填充后的数据
     */
    private static byte[] padding(byte[] src, int blockSize) {
        int paddingLen = blockSize - (src.length + 1) % blockSize;

        if (paddingLen == blockSize) {
            return src;
        }

        // 注意这里的操作，填充的内容就是paddingLen
        byte[] padding = new byte[paddingLen + 1];
        Arrays.fill(padding, (byte)paddingLen);

        return CollectionUtil.merge(src, padding);
    }
}
