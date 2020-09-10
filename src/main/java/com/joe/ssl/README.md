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






- SSLEngineImpl可以看做是SSLSocketImpl的优化版本
- netData是从网络读取到的流量，appData指的应该是用户传入进来的缓冲区，offset和length应该也是用户传进来的，指定最多读取多长，以及写入缓冲区的位置
- internalData：只要不是application_data就是internalData

## 算法套件
### AES
AEAD模式(GCM)：
加密不需要mac算法，自带mac算法（消息认证）；
有fixedIv
GCM有一个tagSize的概念，表示身份验证的长度，目前已知的都是128bit，即16 byte
GCM有一个recordIvSize的概念，recordIvSize = ivSize - fixedIvSize，表示随机数的大小，对于GCM来说，iv = fixedIv + nonce，nonce就是TCP的seqNumber（32 bit）
authenticator直接使用Authenticator，new出来一个对象，传入版本号

BLOCK_CIPHER（CBC）模式：
需要mac算法
没有fixedIv
authenticator使用的是mac算法，如果是客户端，则使用服务端秘钥（MacSecret），如果是服务端，则使用客户端秘钥（MacSecret）


AES算法加密数据长度必须是16（单位byte）的整数倍，长度不足需要填充，如果加密模式选用的是NoPadding那么必须手动填充数据，算法的密钥长度必须是16、24
或者32（单位byte），算法iv长度固定16（单位byte）；

常用填充方式：

- NoPadding
- PKCS7Padding

## 加密数据格式
明文：
- 1 byte的record类型
- 2 byte 版本信息
- 2 byte长度信息，单位byte
- 4 byte的nonce信息（AEAD模式有）

密文：
- 结尾MAClen长度的mac认证信息（AEAD模式没有）

## record层数据结构：
struct {
    uint8 major;
    uint8 minor;
} ProtocolVersion;（2 byte）

enum {
    change_cipher_spec(20), alert(21), handshake(22),
    application_data(23), (255)
} ContentType;（1 byte）

struct {
    ContentType type;（1 byte）
    ProtocolVersion version;（2 byte）
    uint16 length;（TLSPlaintext.fragment的长度，虽然有16bit，但是一般不能超过2^14，因为record必须全部数据接收完才能做解密操作，太长的话需要等很久才能完成一个包的接受）
    opaque fragment[TLSPlaintext.length];（负载数据，部分加密的）
} TLSPlaintext;

## record负载数据TLSPlaintext的数据结构：
struct {
    ContentType type;
    ProtocolVersion version;
    uint16 length;
    select (SecurityParameters.cipher_type) {
        case stream: GenericStreamCipher;
        case block:  GenericBlockCipher;
        case aead:   GenericAEADCipher;
    } fragment;
} TLSCiphertext;

## GenericBlockCipher
struct {
    opaque IV[SecurityParameters.record_iv_length];（实际上是一个blockSize的随机数，用random生成，并不会使用，本项数据会一起加密，cipher初始化使用的iv是密钥交换时的）
    block-ciphered struct {
        opaque content[TLSCompressed.length];（负载数据）
        opaque MAC[SecurityParameters.mac_length];（mac认证信息，这里认证的是认证添加数据+负载数据，认证添加数据 = 8 byte的seqNumber + 1 byte的recordType + 2 byte的protocolVersion + 2 byte的负载数据长度）
        uint8 padding[GenericBlockCipher.padding_length];（padding信息，会加密）
        uint8 padding_length;（padding的长度）
    };
} GenericBlockCipher;


## GenericAEADCipher
struct {
   opaque nonce_explicit[SecurityParameters.record_iv_length];（这里是显式iv，实际的iv是隐式iv（即密钥交换的iv） + 显式iv（即nonce）组成），这部分数据是不加密的
   aead-ciphered struct {
       opaque content[TLSCompressed.length];（负载数据）
   };
} GenericAEADCipher;



com.joe.ssl.openjdk.ssl.CipherBox.createExplicitNonce
com.joe.ssl.openjdk.ssl.CipherBox.applyExplicitNonce


## seqNumber说明
如果是BLOCK模式，那么在计算MAC的时候+1，如果是AEAD模式，那么在初始化cipher（applyExplicitNonce）的时候+1；

## 写出数据
- 如果是CBC模式，需要mac，则对传输数据进行签名计算（此时如果是BLOCK模式，会将seqNumber + 1），然后将签名也放入传输数据中
- 调用createExplicitNonce创建一个随机数，创建算法：如果是BLOCK模式，那么生成一个随机数返回，如果是AEAD模式，使用当前sequenceNumber作为随机数，初始化cipher，使用acquireAuthenticationBytes获得一个添加数据（seqNumber+1），update到cipher中；
- 将nonce放入网络缓冲区（注意：AEAD模式不会加密该nonce，而BLOCK模式会）；
- 加密缓冲区数据，如果是BLOCK模式，先对数据进行padding
  > padding算法：可以参考AesExample#padding
- 开始实际的加密：
  - 如果是AEAD模式，先调用cipher.getOutputSize（要写出的数据长度）来获取加密输出长度，如果缓冲区不够则扩容，然后调用cipher.doFinal进行加密
  - 如果是BLOCK模式，直接调用update来加密（注意校验加密结果长度等于源数据长度）
- 补充缓冲区5个byte的header（TLSCiphertext结构的header）
- 数据写出；


## 读取加密数据步骤
SSLEngineImpl#readNetRecord：
- 检查当前是否需要上报一些SSLException（checkTaskThrown）
- 如果当前读取通道已经关闭，那么返回CLOSE
- 如果当前链接状态还是cs_HANDSHAKE或者cs_START，调用kickstartHandshake开始握手（如果当前是start状态）
- 检查当前握手状态，如果是NEED_WRAP则返回OK，并且返回消费/处理0字节数据；
- 检查当前握手状态，如果是NEED_TASK同上（NEED_TASK表示当前有些必须要处理的任务未完成，例如finish消息要处理更换密码）
- 检查当前网络传输的数据长度，如果不够5byte（因为SSLv2的长度信息是在0、1位置，而SSLv3/TLS的长度信息是在3、4位置，所以这里最少要读到5个byte，才能保证获取到长度信息）则返回-1
- 获取网络数据中的第0byte数据，表示当前record类型（20表示ct_change_cipher_spec，21表示ct_alert，22表示ct_handshake，23表示ct_application_data）；
- 如果是ct_handshake或者ct_alert或者formatVerified（这是什么概念？好像表示的是SSLv3/TLS），那么获取版本号，在网络数据的第1、2byte，检查是否支持，读取长度信息+固定的5byte的头信息长度返回作为packetLen，长度信息在网络数据的第3、4位
- 检查packetLen是否大于当前包的最大大小(33305 byte = maxRecordSize + 16384的maxDataSize， maxRecordSize = headerPlusMaxIVSize + 16384的maxDataSize + 256的maxPadding + 20的trailerSize，headerPlusMaxIVSize = 5的headerSize + 256的maxIVLength，trailerSize是MAC或者AEAD tag)，
- 检查packetLen - Record.headerSize（5 byte）是否大于appData的剩余大小，如果大于说明用户缓冲中存不下了，将会返回BUFFER_OVERFLOW；
- 检查packetLen等于-1或者网络数据剩余未读的小于packetLen，如果成立则返回BUFFER_UNDERFLOW，表示当前包读取不完整；
- 接下来就能调用readRecord真正的读取了；

SSLEngineImpl#readRecord：
- 如果当前链接状态是cs_ERROR，那么直接返回
- 将当前网络数据copy到一个新的缓冲区（readBB），方便操作使用（注意，这里只copy了body，没有copy5个byte的header）
- 将当前网络数据的position和limit设置为原始的limit（position设置为limit表示已经读取完毕了）
- 开始调用decrypt解密数据

EngineInputRecord#decrypt：
- 如果cipher不为空，开始解密，cipheredLength = 当前缓冲区可读长度，如果是BLOCK模式加密tagLen = MAClen，否则等于0
- 如果是AEAD模式加密，那么跳过nonceSize的数据，也就是说明AEAD模式下nonce是不加密传输的
- 开始实际解密，如果是AEAD模式，直接使用doFinal方法，并记录解密结果长度为newLen，如果不是AEAD模式，则调用update方法，并记录结果长度newLen，还要跟加密的数据长度对比，必须一致
- 重置缓冲区的limit
- 如果是BLOCK模式，那么将缓冲区的position置为0，移除padding后返回新的长度newLen（不包含padding，newLen = len - padLen - 1），并且版本大于等于TLS1.1时newLen应该大于等于blockSize，不满足就抛出异常；
  > padding移除算法：获取当前缓冲区数据的最后一个byte，表示的是padLen
- 如果上一步计算的newLen-tagLen小于0那么抛出异常
- 跳过缓冲区nonceSize的数据
- 校验mac，对缓冲区数据进行摘要（不包含mac数据），然后和缓冲区中的认证信息对比，最终将缓冲区的position还原，limit缩小到mac认证信息处，也就是后续就不需要mac认证信息了；
  > 计算认证信息的时候，需要先将添加数据update到mac认证器中，然后才是传输数据；添加认证信息（com.joe.ssl.openjdk.ssl.Authenticator.acquireAuthenticationBytes）：13byte，组成：8 byte的seqNumber + 1 byte的recordType + 2 byte的protocolVersion + 2 byte的record length，其中protocolVersion是先放major，然后放minor，这里recordLen等于要认证的数据的长度（不包含缓冲区中mac认证信息）
- 如果是BLOCK模式，需要对内部buf进行mac校验（为了什么？没太看懂，实际效果就是将sequence number + 1了）
- 返回解密后的数据，不包含头部的nonce和尾部的mac信息（如有）




com.joe.ssl.openjdk.ssl.CipherBox.applyExplicitNonce(com.joe.ssl.openjdk.ssl.Authenticator, byte, java.nio.ByteBuffer)：
如果是BLOCK模式：
- 校验密文完整性
- 返回ivSize

如果是AEAD（GCM）模式：
- 读取nonce（长度固定为4 byte，其实就是TCP的seqNumber）补充到iv中；
- 将缓冲区的读写指针往前调整recordIvSize大小（也就是4byte的nonce大小）
- 初始化cipher；（因为上述操作导致实际的iv变化，所以实际上cipher是要每次都重新初始化的）
- 获取认证信息（实际上是13byte，组成：8 byte的seqNumber + 1 byte的recordType + 2 byte的protocolVersion + 2 byte的record length，其中protocolVersion是先放major，然后放minor，recordLen = 传输数据长度 - recordIvSize - tagSize）
- 将认证信息更新到cipher中
- 返回recordIvSize（nonce的长度，固定4 byte）


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