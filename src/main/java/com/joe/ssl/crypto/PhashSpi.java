package com.joe.ssl.crypto;

/**
 * Phash算法，跟PRF算法的区别就是Phash算法只需要一个seed，PRF需要label+seed，只需要调用{@link #phash(byte[], byte[]) phash}方法
 * 时seed参数传入label+seed即可；
 * 
 * <p>
 *     注意：如果将该算法作为PRF算法，那么仅支持TLS1.2以及更高版本，TLS1.2以下PRF算法跟本算法有区别
 * </p>
 * <br/>
 * <p>
 * &#9;    compute:<br/><br/>
 *
 * &#9;&#9;         P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +<br/>
 * &#9;&#9;                                HMAC_hash(secret, A(2) + seed) +<br/>
 * &#9;&#9;                                HMAC_hash(secret, A(3) + seed) + ...<br/>
 * &#9;     A() is defined as:<br/><br/>
 *
 * &#9;&#9;         A(0) = seed<br/>
 * &#9;&#9;         A(i) = HMAC_hash(secret, A(i-1))<br/>
 * </p>
 *
 * @author JoeKerouac
 * @version 2020年07月27日 15:07
 */
public interface PhashSpi extends AlgorithmSpi<PhashSpi> {

    /**
     * 初始化
     *
     * @param key 对称密钥
     */
    void init(byte[] key);

    /**
     * 计算phash，如果传入的seed是label+seed数据则是PRF算法
     * @param seed 种子
     * @param output 结果输出数组，最终结果将会输出到这里，长度不固定，对于TLS1.0+来说数组长度是12
     */
    void phash(byte[] seed, byte[] output);

    /**
     * hmac算法名
     * @return hash算法名
     */
    String hmacAlgorithm();

    @Override
    default int type() {
        return AlgorithmSpi.PHASH;
    }

    static PhashSpi getInstance(String algorithm) {
        return AlgorithmRegistry.newInstance("alias.phash." + algorithm);
    }
}
