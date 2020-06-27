package com.joe.ssl.message;

/**
 * @author JoeKerouac
 * @version 2020年06月14日 12:26
 */
public class SignatureAndHashAlgorithm {

    enum SignatureAlgorithm {
                             UNDEFINED("undefined", -1),

                             ANONYMOUS("anonymous", 0),

                             RSA("rsa", 1),

                             DSA("dsa", 2),

                             ECDSA("ecdsa", 3);

        final String name;
        final int    value;

        SignatureAlgorithm(String name, int value) {
            this.name = name;
            this.value = value;
        }

        static SignatureAlgorithm valueOf(int var0) {
            SignatureAlgorithm var1 = UNDEFINED;
            switch (var0) {
                case 0:
                    var1 = ANONYMOUS;
                    break;
                case 1:
                    var1 = RSA;
                    break;
                case 2:
                    var1 = DSA;
                    break;
                case 3:
                    var1 = ECDSA;
            }

            return var1;
        }
    }

    enum HashAlgorithm {
                        UNDEFINED("undefined", "", -1, -1),

                        NONE("none", "NONE", 0, -1),

                        MD5("md5", "MD5", 1, 16),

                        SHA1("sha1", "SHA-1", 2, 20),

                        SHA224("sha224", "SHA-224", 3, 28),

                        SHA256("sha256", "SHA-256", 4, 32),

                        SHA384("sha384", "SHA-384", 5, 48),

                        SHA512("sha512", "SHA-512", 6, 64);

        final String name;
        final String standardName;
        final int    value;
        final int    length;

        HashAlgorithm(String name, String standardName, int value, int length) {
            this.name = name;
            this.standardName = standardName;
            this.value = value;
            this.length = length;
        }

        static SignatureAndHashAlgorithm.HashAlgorithm valueOf(int var0) {
            HashAlgorithm var1 = UNDEFINED;
            switch (var0) {
                case 0:
                    var1 = NONE;
                    break;
                case 1:
                    var1 = MD5;
                    break;
                case 2:
                    var1 = SHA1;
                    break;
                case 3:
                    var1 = SHA224;
                    break;
                case 4:
                    var1 = SHA256;
                    break;
                case 5:
                    var1 = SHA384;
                    break;
                case 6:
                    var1 = SHA512;
            }

            return var1;
        }
    }
}
