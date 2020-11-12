package com.joe.tls.util;

import javax.net.ssl.X509ExtendedKeyManager;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-12 15:55
 */
public class CertificateUtil {

    public static Certificate[] readCert(X509ExtendedKeyManager keyManager,String algorithm,InputStream inputStream) {
        try {
            String alias = keyManager.chooseServerAlias(algorithm, null, null);


            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate certificate = cf.generateCertificate(inputStream);

            return null;
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

}
