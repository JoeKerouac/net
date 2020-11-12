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
 * 自定义实现的密钥管理
 * 
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
    public String[] getClientAliases(String keyType, Principal[] principals) {
        return getAliases(keyType, principals);
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] principals, Socket socket) {
        if (keyTypes == null) {
            return null;
        }

        for (String keyType : keyTypes) {
            String[] aliases = getClientAliases(keyType, principals);
            if (aliases != null && aliases.length > 0) {
                return aliases[0];
            }
        }

        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] principals) {
        return getAliases(keyType, principals);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] principals, Socket socket) {
        if (keyType == null) {
            return null;
        }

        String[] aliases;

        if (principals == null || principals.length == 0) {
            aliases = serverAliasCache.get(keyType);
            if (aliases == null) {
                aliases = getServerAliases(keyType, principals);

                if (aliases == null) {
                    aliases = new String[0];
                }

                serverAliasCache.put(keyType, aliases);
            }
        } else {
            aliases = getServerAliases(keyType, principals);
        }

        if (aliases != null && aliases.length > 0) {
            return aliases[0];
        }

        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        if (alias == null) {
            return null;
        }

        X509Credentials cred = credentialsMap.get(alias);
        if (cred == null) {
            return null;
        } else {
            return cred.certificates.clone();
        }
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        if (alias == null) {
            return null;
        }

        X509Credentials cred = credentialsMap.get(alias);
        if (cred == null) {
            return null;
        } else {
            return cred.privateKey;
        }
    }

    /**
     * 获取指定公钥类型的别名，并且该公钥需要被认可（如果有），认可的证书颁发机构在principals中保存
     * @param keyType 公钥类型
     * @param principals 认可的证书颁发机构列表
     * @return 认可的颁发机构颁发的指定类型的证书
     */
    private String[] getAliases(String keyType, Principal[] principals) {
        if (keyType == null) {
            return null;
        }

        if (principals == null) {
            principals = new X500Principal[0];
        }

        if (!(principals instanceof X500Principal[])) {
            principals = tryConvert(principals);
        }

        String sigType;
        if (keyType.contains("_")) {
            int k = keyType.indexOf("_");
            sigType = keyType.substring(k + 1);
            keyType = keyType.substring(0, k);
        } else {
            sigType = null;
        }

        X500Principal[] x500Principals = (X500Principal[]) principals;
        List<String> aliases = new ArrayList<>();

        for (Map.Entry<String, X509Credentials> entry : credentialsMap.entrySet()) {
            String alias = entry.getKey();
            X509Credentials credentials = entry.getValue();
            X509Certificate[] certs = credentials.certificates;

            if (!keyType.equals(certs[0].getPublicKey().getAlgorithm())) {
                continue;
            }

            // 如果签名算法不为空，则检查签名算法
            if (sigType != null) {
                if (certs.length > 1) {
                    // 检查发行者证书中的公钥
                    if (!sigType.equals(certs[1].getPublicKey().getAlgorithm())) {
                        continue;
                    }
                } else {
                    // 检查证书本身的签名算法，在SHA1withRSA中寻找withRSA，依次类推
                    String sigAlgName = certs[0].getSigAlgName().toUpperCase(Locale.ENGLISH);
                    String pattern = "WITH" + sigType.toUpperCase(Locale.ENGLISH);
                    if (!sigAlgName.contains(pattern)) {
                        continue;
                    }
                }
            }

            // 检查认证信息
            if (x500Principals.length == 0) {
                aliases.add(alias);
            } else {
                Set<X500Principal> certIssuers = credentials.getIssuerX500Principals();
                for (X500Principal x500Principal : x500Principals) {
                    if (certIssuers.contains(x500Principal)) {
                        aliases.add(alias);
                        break;
                    }
                }
            }
        }

        String[] aliasStrings = aliases.toArray(new String[0]);
        return aliasStrings.length == 0 ? null : aliasStrings;
    }

    /**
     * 尝试将Principal转换为X500Principal，如果无法转换则抛出异常
     * @param principals principal
     * @return X500Principal
     */
    private X500Principal[] tryConvert(Principal[] principals) {
        X500Principal[] x500Principals = new X500Principal[principals.length];
        for (int i = 0; i < principals.length; i++) {
            if (principals[i] instanceof X500Principal) {
                x500Principals[i] = (X500Principal) principals[i];
            } else {
                throw new RuntimeException("不支持的Principal类型：" + principals[i].getClass());
            }
        }
        return x500Principals;
    }

    /**
     * X509证书
     */
    private static class X509Credentials {

        /**
         * 证书的私钥
         */
        PrivateKey                 privateKey;

        /**
         * 证书链
         */
        X509Certificate[]          certificates;

        /**
         * 证书相关认证信息，包含颁发机构
         */
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
