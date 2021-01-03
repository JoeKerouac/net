package com.joe.tls.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

/**
 * 此处定义的命名曲线是SEC 2 [13]中指定的曲线；详情参照：RFC 4492 5.1.1
 *
 * @author JoeKerouac
 * @version 2020年06月27日 17:20
 */
public class NamedCurve {

    // 支持的全部椭圆曲线
    private static final Map<Integer, NamedCurve> ALL = new HashMap<>();

    static {
        ALL.put(1, new NamedCurve(1, "sect163k1"));
        ALL.put(2, new NamedCurve(2, "sect163r1"));
        ALL.put(3, new NamedCurve(3, "sect163r2"));
        ALL.put(4, new NamedCurve(4, "sect193r1"));
        ALL.put(5, new NamedCurve(5, "sect193r2"));
        ALL.put(6, new NamedCurve(6, "sect233k1"));
        ALL.put(7, new NamedCurve(7, "sect233r1"));
        ALL.put(8, new NamedCurve(8, "sect239k1"));
        ALL.put(9, new NamedCurve(9, "sect283k1"));
        ALL.put(10, new NamedCurve(10, "sect283r1"));
        ALL.put(11, new NamedCurve(11, "sect409k1"));
        ALL.put(12, new NamedCurve(12, "sect409r1"));
        ALL.put(13, new NamedCurve(13, "sect571k1"));
        ALL.put(14, new NamedCurve(14, "sect571r1"));
        ALL.put(15, new NamedCurve(15, "secp160k1"));
        ALL.put(16, new NamedCurve(16, "secp160r1"));
        ALL.put(17, new NamedCurve(17, "secp160r2"));
        ALL.put(18, new NamedCurve(18, "secp192k1"));
        ALL.put(19, new NamedCurve(19, "secp192r1"));
        ALL.put(20, new NamedCurve(20, "secp224k1"));
        ALL.put(21, new NamedCurve(21, "secp224r1"));
        ALL.put(22, new NamedCurve(22, "secp256k1"));
        ALL.put(23, new NamedCurve(23, "secp256r1"));
        ALL.put(24, new NamedCurve(24, "secp384r1"));
        ALL.put(25, new NamedCurve(25, "secp521r1"));
    }

    /**
     * 曲线ID
     */
    private int id;

    /**
     * 曲线名
     */
    private String name;

    private NamedCurve(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getCurveName(int curveId) {
        NamedCurve namedCurveObj = ALL.get(curveId);
        if (namedCurveObj == null) {
            return null;
        }

        return namedCurveObj.getName();
    }

    /**
     * 根据曲线ID获取ECDomainParameters
     *
     * @param curveId
     *            曲线ID
     * @return ECDomainParameters
     */
    public static ECDomainParameters getECParameters(int curveId) {
        // 使用Bouncy Castle实现，支持的椭圆曲线：https://github.com/bcgit/bc-java/wiki/Support-for-ECDSA,-ECGOST-Curves.
        // 支持的椭圆曲线定义：org.bouncycastle.asn1.sec.SECNamedCurves
        // 支持的椭圆曲线定义：org.bouncycastle.asn1.x9.X962NamedCurves
        // 支持的椭圆曲线定义：org.bouncycastle.asn1.nist.NISTNamedCurves
        // 支持的椭圆曲线定义：org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves

        NamedCurve namedCurveObj = ALL.get(curveId);
        if (namedCurveObj == null) {
            return null;
        }

        String curveName = namedCurveObj.getName();

        // Lazily created the first time a particular curve is accessed
        X9ECParameters ecP = SECNamedCurves.getByName(curveName);

        if (ecP == null) {
            return null;
        }

        // It's a bit inefficient to do this conversion every time
        return new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());
    }

    /**
     * 获取所有支持的曲线类型副本
     *
     * @return 所有曲线副本
     */
    public static List<NamedCurve> getAllSupportCurve() {
        return new ArrayList<>(ALL.values());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
