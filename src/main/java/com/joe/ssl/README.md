## 密码套件定义
```
CipherSuite TLS_ECDH_ECDSA_WITH_NULL_SHA = {0xC0，0x01}
CipherSuite TLS_ECDH_ECDSA_WITH_RC4_128_SHA = {0xC0，0x02}
CipherSuite TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA = {0xC0，0x03}
CipherSuite TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA = {0xC0，0x04}
CipherSuite TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA = {0xC0，0x05}

CipherSuite TLS_ECDHE_ECDSA_WITH_NULL_SHA = {0xC0，0x06}
CipherSuite TLS_ECDHE_ECDSA_WITH_RC4_128_SHA = {0xC0，0x07}
CipherSuite TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA = {0xC0，0x08}
CipherSuite TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA = {0xC0，0x09}
CipherSuite TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA = {0xC0，0x0A}

CipherSuite TLS_ECDH_RSA_WITH_NULL_SHA = {0xC0，0x0B}
CipherSuite TLS_ECDH_RSA_WITH_RC4_128_SHA = {0xC0，0x0C}
CipherSuite TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA = {0xC0，0x0D}
CipherSuite TLS_ECDH_RSA_WITH_AES_128_CBC_SHA = {0xC0，0x0E}
CipherSuite TLS_ECDH_RSA_WITH_AES_256_CBC_SHA = {0xC0，0x0F}

CipherSuite TLS_ECDHE_RSA_WITH_NULL_SHA = {0xC0，0x10}
CipherSuite TLS_ECDHE_RSA_WITH_RC4_128_SHA = {0xC0，0x11}
CipherSuite TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA = {0xC0，0x12}
CipherSuite TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA = {0xC0，0x13}
CipherSuite TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA = {0xC0，0x14}

CipherSuite TLS_ECDH_anon_WITH_NULL_SHA = {0xC0，0x15}
CipherSuite TLS_ECDH_anon_WITH_RC4_128_SHA = {0xC0，0x16}
CipherSuite TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA = {0xC0，0x17}
CipherSuite TLS_ECDH_anon_WITH_AES_128_CBC_SHA = {0xC0，0x18}
CipherSuite TLS_ECDH_anon_WITH_AES_256_CBC_SHA = {0xC0，0x19}
```



下面都是基于TLS1.2的内容，使用ECDH密钥交换算法

client接到ServerHello需要处理的事情：
1、记录服务端随机数；
2、记录服务端响应（选择）的版本号
3、记录服务端响应（选择）的算法套件；



client接到Certificate需要处理的事情：
1、证书链验证；
2、serverKey记录为服务端发送的证书链的第一个证书，用于后边验签使用（参考ECDH密钥交换）；


client接到ServerKeyExchange消息后需要处理的事情：
1、确定密钥交换算法；
2、密钥交换，详细交换算法见算法细节，只介绍ECDH密钥交换算法；



client接到ServerHelloDone需要处理的事情：
1、判断是否需要发送客户端的密钥链（证书链），如果需要则发送；
2、发送ClientKeyExchange消息（这里固定使用ECDH算法，发送客户端ECDH公钥）
3、根据客户端私钥和服务端公钥计算preMasterKey；
4、根据preMasterKey计算masterKey（详见算法细节1）；
5、根据masterKey计算链接需要的macKey、cipherKey、iv（详见算法细节2）；






## 算法套件
### AES
AHEAD模式(GCM)：
加密不需要mac算法，自带mac算法（消息认证）；
有fixedIv
GCM有一个tagSize的概念，表示身份验证的长度，目前已知的都是128bit，即16 byte
GCM有一个recordIvSize的概念，recordIvSize = ivSize - fixedIvSize，表示随机数的大小，对于GCM来说，iv = fixedIv + nonce，nonce就是TCP的seqNumber（32 bit）


BLOCK_CIPHER（CBC）模式：
需要mac算法
没有fixedIv


AES算法加密数据长度必须是16（单位byte）的整数倍，长度不足需要填充，如果加密模式选用的是NoPadding那么必须手动填充数据，算法的密钥长度必须是16、24
或者32（单位byte），算法iv长度固定16（单位byte）；

常用填充方式：

- NoPadding
- PKCS7Padding


## 算法细节：
一、根据preMasterKey计算masterKey：
官方算法：com.sun.crypto.provider.TlsMasterSecretGenerator
实现逻辑：使用PRF算法的结果作为masterKey
PRF入参：
label是"master secret"
secret是preMasterKey
seed是clientRandom+serverRandom，注意这两个是有序的，先client然后server
输出长度是48，输出作为masterKey

二、根据masterKey计算链接需要的macKey、cipherKey、iv；
官方算法：com.sun.crypto.provider.TlsKeyMaterialGenerator

同样采用PRF算法计算得出，生成加密密钥key、mackey的PRF算法的参数：
label是com.sun.crypto.provider.TlsPrfGenerator#LABEL_KEY_EXPANSION
secret（密钥）是masterSecret（通过preSecret计算的那个）
seed是serverRandom+clientRandom
输出长度是（macLen + cipherKeyLen + ivLen ） * 2
其中macLen、cipherKeyLen、ivLen都是根据具体算法定的；
ivLen来源com.joe.ssl.openjdk.ssl.CipherSuite.BulkCipher#ivSize，另外如果是AEAD_CIPHER类型那么则会使用fixIvSize

PRF结果包含以下内容：
| 长度macLen的clientMacKey | 长度macLen的serverMacKey | 长度cipherKeyLen的clientCipherKey | 长度cipherKeyLen的serverCipherKey | 长度ivLen的clientIv | 长度ivLen的serverIv |


三、ECDH密钥交换算法详情：
官方算法：com.joe.ssl.openjdk.ssl.HandshakeMessage.ECDH_ServerKeyExchange.ECDH_ServerKeyExchange(com.joe.ssl.openjdk.ssl.HandshakeInStream, java.security.PublicKey, byte[], byte[], java.util.Collection<com.joe.ssl.openjdk.ssl.SignatureAndHashAlgorithm>, com.joe.ssl.openjdk.ssl.ProtocolVersion)
1、首先读取服务端的ECDHParams（就是曲线的类型，选定曲线类型后曲线参数是固定的），先是8个字节的curveType，判断是否是CURVE_NAMED_CURVE（值为3）类型，如果不是抛出异常；
2、读取16个字节的curveId，根据curveId找到对应的ECParameterSpec（这个主要为了用来还原生成服务端的EC参数）；
3、读取8个字节的长度信息（单位byte），然后读取该长度的byte数组作为ECpoint数据，然后根据point数据和上边读出的椭圆曲线类型对应的参数（ECParameterSpec）解析出服务端的ECPoint；
4、根据该服务端的ECPoint信息获取出公钥信息，这个公钥用于后边DH协商公私钥使用，详细算法见sun.security.ec.ECKeyFactory.engineGeneratePublic；
5、如果serverKey（Certificate步骤中获取的）为空，则不继续，如果不为空则使用该公钥进行验签；
6、开始验签，先读取8个字节的hash算法id，然后读取8个字节的签名算法id，根据这两个id获取出来签名器，然后使用serverKey初始化签名器；
7、读取服务端发来的签名信息，读取方式：先读取16个字节的长度信息（单位byte），然后根据该长度信息读取相应长度的byte数组作为签名；
8、开始更新签名器，先更新clientRandom，然后更新serverRandom，然后更新curveType，然后更新curveId，然后更新point数组长度，然后更新point数据；
9、使用上边的签名器验证签名，对比本地生成签名与服务端生成签名是否一致；


A = a * G;
B = b * G;

Qa = a * B = a * b * G;
Qb = b * A = b * a * G;

Qa = Qb