package com.joe.ssl.util;

/**
 * BC里边常用的签名算法
 *
 * @author JoeKerouac
 * @version 2020年06月14日 17:38
 */
public enum SignatureAlgorithm {

                                SHA512withRSA("SHA512withRSA", 0x0601),

    ;

    private String signatureAlgorithm;

    private int    id;

    SignatureAlgorithm(String signatureAlgorithm, int id) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.id = id;
    }

    /**
     * Getter method for property <tt>signatureAlgorithm</tt>
     *
     * @return property value of signatureAlgorithm
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Getter method for property <tt>id</tt>
     *
     * @return property value of id
     */
    public int getId() {
        return id;
    }}
