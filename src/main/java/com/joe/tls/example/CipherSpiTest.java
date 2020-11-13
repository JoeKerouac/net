package com.joe.tls.example;

import java.security.SecureRandom;
import java.util.Arrays;

import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.crypto.CipherSpi;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 11:56
 */
public class CipherSpiTest {

    public static void main(String[] args) throws Exception {
        for (CipherSuite.CipherDesc value : CipherSuite.CipherDesc.values()) {
            test(value);
        }
    }

    /**
     * 校验cipher是否符合预期
     * @param desc 加密算法说明
     * @throws Exception 异常
     */
    private static void test(CipherSuite.CipherDesc desc) throws Exception {
        byte[] key = new byte[desc.getKeySize()];
        byte[] iv = new byte[desc.getIvLen()];

        CipherSpi cipherSpi = CipherSpi.getInstance(desc);
        byte[] data = new byte[cipherSpi.getBlockSize() * 3];
        Arrays.fill(data, (byte) 3);

        byte[] encryptResult = valid(desc, key, iv, CipherSpi.ENCRYPT_MODE, data);
        byte[] decryptResult = valid(desc, key, iv, CipherSpi.DECRYPT_MODE, encryptResult);

        if (!Arrays.equals(data, decryptResult)) {
            throw new RuntimeException("加解密后数据不一致");
        }
    }

    /**
     * 对同一组数据调用不同处理方法验证结果一致，验证成功后将结果返回
     * @param desc 加密算法说明
     * @param key key
     * @param iv iv
     * @param mode 加解密模式
     * @param data 要加解密的数据
     * @return 结果
     */
    private static byte[] valid(CipherSuite.CipherDesc desc, byte[] key, byte[] iv, int mode,
                                byte[] data) {
        byte[] result1 = updateAndDoFinal(desc, key, iv, mode, data);
        byte[] result2 = updateAndDoFinal(desc, key, iv, mode, data);
        byte[] result3 = updateSplitAndDoFinal(desc, key, iv, mode, data);
        byte[] result4 = updateSplitAndDoFinal(desc, key, iv, mode, data);
        byte[] result5 = doFinal(desc, key, iv, mode, data);
        byte[] result6 = doFinal(desc, key, iv, mode, data);

        if (!Arrays.equals(result1, result2) || !Arrays.equals(result1, result3)
            || !Arrays.equals(result1, result4) || !Arrays.equals(result1, result5)
            || !Arrays.equals(result1, result6)) {
            System.out.println(Arrays.toString(result1));
            System.out.println(Arrays.toString(result2));
            System.out.println(Arrays.toString(result3));
            System.out.println(Arrays.toString(result4));
            System.out.println(Arrays.toString(result5));
            System.out.println(Arrays.toString(result6));
            throw new RuntimeException("验证失败，加密模式：" + desc + "；当前mode：" + mode);
        }

        return result1;
    }

    /**
     * 直接调用doFinal得到结果
     * @param desc 加密算法说明
     * @param key key
     * @param iv iv
     * @param mode 加解密模式
     * @param data 要加解密的数据
     * @return 结果
     */
    private static byte[] doFinal(CipherSuite.CipherDesc desc, byte[] key, byte[] iv, int mode,
                                  byte[] data) {
        CipherSpi cipherSpi = CipherSpi.getInstance(desc);
        cipherSpi.init(key, iv, mode, new SecureRandom());
        return cipherSpi.doFinal(data);
    }

    /**
     * 先将数据一次性update到cipher，然后获取结果的方法
     * @param desc 加密算法说明
     * @param key key
     * @param iv iv
     * @param mode 加解密模式
     * @param data 要加解密的数据
     * @return 结果
     */
    private static byte[] updateAndDoFinal(CipherSuite.CipherDesc desc, byte[] key, byte[] iv,
                                           int mode, byte[] data) {
        CipherSpi cipherSpi = CipherSpi.getInstance(desc);
        cipherSpi.init(key, iv, mode, new SecureRandom());
        byte[] result = cipherSpi.update(data);
        byte[] finalResult = cipherSpi.doFinal();
        if (finalResult.length != 0) {
            byte[] b = new byte[result.length + finalResult.length];
            System.arraycopy(result, 0, b, 0, result.length);
            System.arraycopy(finalResult, 0, b, result.length, finalResult.length);
            result = b;
        }
        return result;
    }

    /**
     * 将数据拆分多组update到cipher，然后获取结果
     * @param desc 加密算法说明
     * @param key key
     * @param iv iv
     * @param mode 加解密模式
     * @param data 要加解密的数据
     * @return 结果
     */
    private static byte[] updateSplitAndDoFinal(CipherSuite.CipherDesc desc, byte[] key, byte[] iv,
                                                int mode, byte[] data) {
        CipherSpi cipherSpi = CipherSpi.getInstance(desc);
        cipherSpi.init(key, iv, mode, new SecureRandom());

        // 得到多个结果，注意，这里java自带SunJCE实现和BouncyCastleProvider有区别：
        // SunJCE实现的AES解密在这里返回的result1-result4都是空数组，最后doFinal才会返回结果；
        // BouncyCastleProvider实现的AES解密在这里分段解密的时候result1是空的，result2、result3、result4拼接起来就是结果了，最后doFinal则会返回空数组
        byte[] result1 = cipherSpi.update(Arrays.copyOfRange(data, 0, cipherSpi.getBlockSize()));
        byte[] result2 = cipherSpi.update(
            Arrays.copyOfRange(data, cipherSpi.getBlockSize(), cipherSpi.getBlockSize() * 2));
        byte[] result3 = cipherSpi.update(
            Arrays.copyOfRange(data, cipherSpi.getBlockSize() * 2, cipherSpi.getBlockSize() * 3));
        byte[] result4 = cipherSpi
            .update(Arrays.copyOfRange(data, cipherSpi.getBlockSize() * 3, data.length));

        byte[] result = new byte[result1.length + result2.length + result3.length + result4.length];
        System.arraycopy(result1, 0, result, 0, result1.length);
        System.arraycopy(result2, 0, result, result1.length, result2.length);
        System.arraycopy(result3, 0, result, result1.length + result2.length, result3.length);
        System.arraycopy(result4, 0, result, result1.length + result2.length + result3.length,
            result4.length);

        byte[] finalResult = cipherSpi.doFinal();
        if (finalResult.length != 0) {
            byte[] b = new byte[result.length + finalResult.length];
            System.arraycopy(result, 0, b, 0, result.length);
            System.arraycopy(finalResult, 0, b, result.length, finalResult.length);
            result = b;
        }
        return result;
    }

}
