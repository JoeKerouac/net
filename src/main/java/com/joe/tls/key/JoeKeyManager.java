package com.joe.tls.key;

import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.X509KeyManager;
import javax.security.auth.x500.X500Principal;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-12 16:24
 */
public class JoeKeyManager implements X509KeyManager {

    private Map<String, X509Credentials> credentialsMap;

    private Map<String, String[]>        serverAliasCache;

    public JoeKeyManager(KeyStore ks, char[] password) throws KeyStoreException,
                                                       NoSuchAlgorithmException,
                                                       UnrecoverableKeyException {
        credentialsMap = new HashMap<>();
        serverAliasCache = new ConcurrentHashMap<>();

        if (ks == null) {
            return;
        }

        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!ks.isKeyEntry(alias)) {
                continue;
            }
            Key key = ks.getKey(alias, password);
            if (!(key instanceof PrivateKey)) {
                continue;
            }

            Certificate[] certs = ks.getCertificateChain(alias);
            // 只能解析X509格式的证书
            if (certs == null || certs.length == 0 || !(certs[0] instanceof X509Certificate)) {
                continue;
            }

            X509Credentials cred = new X509Credentials((PrivateKey) key, (X509Certificate[]) certs);
            credentialsMap.put(alias, cred);
        }
    }

    @Override
    public String[] getClientAliases(String s, Principal[] principals) {
        return new String[0];
    }

    @Override
    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
        return null;
    }

    @Override
    public String[] getServerAliases(String s, Principal[] principals) {
        return new String[0];
    }

    @Override
    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String s) {
        return new X509Certificate[0];
    }

    @Override
    public PrivateKey getPrivateKey(String s) {
        return null;
    }

    private static class X509Credentials {
        PrivateKey                 privateKey;
        X509Certificate[]          certificates;
        private Set<X500Principal> issuerX500Principals;

        X509Credentials(PrivateKey privateKey, X509Certificate[] certificates) {
            // assert privateKey and certificates != null
            this.privateKey = privateKey;
            this.certificates = certificates;
        }

        synchronized Set<X500Principal> getIssuerX500Principals() {
            // lazy initialization
            if (issuerX500Principals == null) {
                issuerX500Principals = new HashSet<X500Principal>();
                for (int i = 0; i < certificates.length; i++) {
                    issuerX500Principals.add(certificates[i].getIssuerX500Principal());
                }
            }
            return issuerX500Principals;
        }
    }
}
