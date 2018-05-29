package com.tangem.domain.wallet;

import android.util.Log;

import com.tangem.util.Util;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.asn1.x9.X9IntegerConverter;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECAlgorithms;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static org.bitcoinj.core.ECKey.CURVE;
import static org.bitcoinj.core.ECKey.HALF_CURVE_ORDER;

/**
 * Created by Ilia on 15.02.2018.
 */

public class CryptoUtil {

    public static boolean checkHashSign2(byte[] pub, byte[] hash, BigInteger r, BigInteger s)
    {
        ECDSASigner signer = new ECDSASigner();

        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        return signer.verifySignature(hash, r, s);
    }

    public static boolean isCanonical(BigInteger s) {
        return s.compareTo(HALF_CURVE_ORDER) <= 0;
    }

    public static BigInteger toCanonicalised(BigInteger s) {

        // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
        // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
        //    N = 10
        //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
        //    10 - 8 == 2, giving us always the latter solution, which is canonical.
        if(!isCanonical(s)) {
            BigInteger canon = CURVE.getN().subtract(s);
            Log.e("TX_SIGN", "non Canonical S");
            return canon;
        }

        return s;

    }

    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }

    public static byte[] recoverPubBytesFromSignature(int recId, ECDSASignature_ETH sig, byte[] messageHash) {
        // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
        //   1.1 Let x = r + jn

        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters CURVE2 = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());

        BigInteger n = CURVE2.getN();  // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.r.add(i.multiply(n));
        //   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
        //        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
        //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
        //        conversion routine specified in Section 2.3.4. If this conversion routine outputs “invalid”, then
        //        do another iteration of Step 1.
        //
        // More concisely, what these points mean is to use X as a compressed public key.
        ECCurve.Fp curve = (ECCurve.Fp) CURVE2.getCurve();
        BigInteger prime = curve.getQ();  // Bouncy Castle is not consistent about the letter it uses for the prime.
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null;
        }
        // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
        // So it's encoded in the recId.
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
        if (!R.multiply(n).isInfinity())
            return null;
        //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        BigInteger e = new BigInteger(1, messageHash);
        //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
        //   1.6.1. Compute a candidate public key as:
        //               Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
        // ** is point multiplication and + is point addition (the EC group operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
        // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.r.modInverse(n);
        BigInteger srInv = rInv.multiply(sig.s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(CURVE2.getG(), eInvrInv, R, srInv);
        return q.getEncoded(/* compressed */ false);
    }

    public static byte[] calcSign(byte[] priv, byte[] hash)
    {
        ECDSASigner signer = new ECDSASigner();
        BigInteger d = new BigInteger(priv);
        ECPrivateKeyParameters params = new ECPrivateKeyParameters(d, CURVE);
        signer.init(true, params);
        BigInteger[] rs = signer.generateSignature(hash);
        byte[] r = rs[0].toByteArray();
        byte[] s = rs[1].toByteArray();
        byte[] sign = new byte[64];
        for(int i = 0; i < 32; ++i)
        {
            sign[i] = r[i];
            sign[i+32] = s[i];
        }
        return sign;

    }

    public static boolean isEncodingCanonical(byte[] signature) {
        // See Bitcoin Core's IsCanonicalSignature, https://bitcointalk.org/index.php?topic=8392.msg127623#msg127623
        // A canonical signature exists of: <30> <total len> <02> <len R> <R> <02> <len S> <S> <hashtype>
        // Where R and S are not negative (their first byte has its highest bit not set), and not
        // excessively padded (do not start with a 0 byte, unless an otherwise negative number follows,
        // in which case a single 0 byte is necessary and even required).
        if (signature.length < 9 || signature.length > 73)
            return false;

        int hashType = (signature[signature.length-1] & 0xff) & ~0x80; // mask the byte to prevent sign-extension hurting us
        if (hashType < 1 || hashType > 3)
            return false;

        //                   "wrong type"                  "wrong length marker"
        if ((signature[0] & 0xff) != 0x30 || (signature[1] & 0xff) != signature.length-3)
            return false;

        int lenR = signature[3] & 0xff;
        if (5 + lenR >= signature.length || lenR == 0)
            return false;
        int lenS = signature[5+lenR] & 0xff;
        if (lenR + lenS + 7 != signature.length || lenS == 0)
            return false;

        //    R value type mismatch          R value negative
        if (signature[4-2] != 0x02 || (signature[4] & 0x80) == 0x80)
            return false;
        if (lenR > 1 && signature[4] == 0x00 && (signature[4+1] & 0x80) != 0x80)
            return false; // R value excessively padded

        //       S value type mismatch                    S value negative
        if (signature[6 + lenR - 2] != 0x02 || (signature[6 + lenR] & 0x80) == 0x80)
            return false;
        if (lenS > 1 && signature[6 + lenR] == 0x00 && (signature[6 + lenR + 1] & 0x80) != 0x80)
            return false; // S value excessively padded

        return true;
    }


    public static boolean VerifySign(byte[] tlvPublicKey, byte[] data, byte[] tlvSignature) throws IOException, SignatureException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory factory = KeyFactory.getInstance("EC", "SC");

        ECPoint p1 = spec.getCurve().decodePoint(tlvPublicKey);
        ECPublicKeySpec keySpec = new ECPublicKeySpec(p1, spec);

        PublicKey publicKey = factory.generatePublic(keySpec);
        signature.initVerify(publicKey);
        signature.update(data);

        ASN1EncodableVector v = new ASN1EncodableVector();
        int size = tlvSignature.length / 2;
        v.add(/*r*/new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(tlvSignature, 0, size))));
        v.add(/*s*/new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(tlvSignature, size, size * 2))));
        byte[] sigDer = new DERSequence(v).getEncoded();

        return signature.verify(sigDer);
    }

    private static byte[] leaderZero(byte[] s)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (s[0] > 0x7f) {
            baos.write((byte) 0x00);
        }
        for(int i = 0; i < 32; ++i)
            baos.write(s[i]);

        return baos.toByteArray();
    }
    public static boolean checkHashSign(byte[] pub, byte[] hash, byte[] sign)
    {
        byte[] rtmp = new byte[32];
        byte[] stmp = new byte[32];

        for(int i = 0; i< 32; ++i)
        {
            rtmp[i] = sign[i];
            stmp[i] = sign[i+32];
        }

        leaderZero(rtmp);
        byte[] r2 = leaderZero(rtmp);
        byte[] s2 = leaderZero(stmp);

        BigInteger r = new BigInteger(r2);
        BigInteger s = new BigInteger(s2);
        ECDSASigner signer = new ECDSASigner();

        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        return signer.verifySignature(hash, r, s);
    }
    public static byte[] doubleSha256(byte[] bytes) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(sha256.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha256ripemd160(byte[] publicKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] sha256hash = sha256.digest(publicKey);
            byte[] hashedPublicKey =  Util.calculateRIPEMD160(sha256hash);
            return hashedPublicKey;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        return null;
    }
}
