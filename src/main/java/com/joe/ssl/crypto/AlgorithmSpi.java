package com.joe.ssl.crypto;

/**
 * 算法接口
 * 
 * @author JoeKerouac
 * @version 2020年07月23日 16:00
 */
public interface AlgorithmSpi extends Cloneable {

    /**
     * 算法名
     * @return 算法名
     */
    String name();

    Object clone() throws CloneNotSupportedException;
}
