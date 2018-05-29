package com.tangem.domain.cardReader;

import android.util.Log;

import com.tangem.util.Util;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by dvol on 14.11.2017.
 */

public class CardCrypto {
    public static PublicKey LoadPublicKey(byte[] publicKeyArray) throws Exception {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory factory = KeyFactory.getInstance("EC", "SC");

        ECPoint p1 = spec.getCurve().decodePoint(publicKeyArray);
        ECPublicKeySpec keySpec = new ECPublicKeySpec(p1, spec);

        return factory.generatePublic(keySpec);
    }

    public static boolean VerifySignature(byte[] publicKeyArray, byte[] data, byte[] signature) throws Exception {
        Signature signatureInstance = Signature.getInstance("SHA256withECDSA");
        PublicKey publicKey = LoadPublicKey(publicKeyArray);
        signatureInstance.initVerify(publicKey);
        signatureInstance.update(data);

        ASN1EncodableVector v = new ASN1EncodableVector();
        int size = signature.length / 2;
        v.add(/*r*/new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(signature, 0, size))));
        v.add(/*s*/new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(signature, size, size * 2))));
        byte[] sigDer = new DERSequence(v).getEncoded();

        return signatureInstance.verify(sigDer);
    }


//    public static boolean isCanonical(BigInteger s) {
//
//        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
//
//        BigInteger HALF_CURVE_ORDER = spec.getN().shiftRight(1);
//        return s.compareTo(HALF_CURVE_ORDER) <= 0;
//    }
//
//    public static BigInteger toCanonicalised(BigInteger s) {
//
//        // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
//        // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
//        //    N = 10
//        //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
//        //    10 - 8 == 2, giving us always the latter solution, which is canonical.
//        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
//        if(!isCanonical(s)) {
//            BigInteger canon = spec.getN().subtract(s);
//            //Log.e("TX_SIGN", "non Canonical S");
//            return canon;
//        }
//
//        return s;
//
//    }
//
//    public static BigInteger[] calcSign2(byte[] priv, byte[] hash)
//    {
//        ECDSASigner signer = new ECDSASigner();
//        BigInteger d = new BigInteger(priv);
//
//        ECNamedCurveParameterSpec CURVE_PARAMS = ECNamedCurveTable.getParameterSpec("secp256k1");
//        ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
//                CURVE_PARAMS.getH());
//        ECPrivateKeyParameters params = new ECPrivateKeyParameters(d, CURVE);
//        //ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
//        signer.init(true, params);
//        BigInteger[] rs = signer.generateSignature(hash);
//        return rs;
//
//    }
//
//    public static byte[] Signature3(byte[] privateKeyArray, byte[] data) throws Exception
//    {
//        byte[] hash=Util.calculateSHA256(data);
//        BigInteger[] signBI=calcSign2(privateKeyArray, hash);
//        //signBI[0]=toCanonicalised(signBI[0]);
//        signBI[1]=toCanonicalised(signBI[1]);
//        byte[] r = signBI[0].toByteArray();
//        byte[] s = signBI[1].toByteArray();
//
//        byte[] res = new byte[64];
//        if( r.length==32 ) {
//            System.arraycopy(r, 0, res, 0, r.length);
//        }else if( r.length==33 && r[0]==0 ){
//            System.arraycopy(r, 1, res, 0, r.length-1);
//        }else {
//            throw new Exception("unsupported r-length");
//        }
//        if( s.length==32 ) {
//            System.arraycopy(s, 0, res, 32, 32);
//        }else{
//            throw new Exception("unsupported s-length");
//        }
//        return  res;
//    }

    public static byte[] Signature(byte[] privateKeyArray, byte[] data) throws Exception {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory factory = KeyFactory.getInstance("EC", "SC");

        ECPrivateKeySpec keySpecP = new ECPrivateKeySpec(new BigInteger(1,privateKeyArray), spec);

        Signature signature = Signature.getInstance("SHA256withECDSA");

        PrivateKey privateKey = factory.generatePrivate(keySpecP);
        signature.initSign(privateKey);
        signature.update(data);
        byte[] enc = signature.sign();

        if (enc[0] != 0x30) throw new Exception("bad encoding 1");
        if ((enc[1] & 0x80) != 0) throw new Exception("unsupported length encoding 1");
        if (enc[2] != 0x02) throw new Exception("bad encoding 2");
        if ((enc[3] & 0x80) != 0) throw new Exception("unsupported length encoding 2");
        int rLength = enc[3];

        if (enc[4 + rLength] != 0x02) throw new Exception("bad encoding 3");
        if ((enc[5 + rLength] & 0x80) != 0) throw new Exception("unsupported length encoding 3");
        int sLength = enc[5 + rLength];


        int sPos = 6 + rLength;
        byte[] res = new byte[64];
        if (rLength <= 32) {
            System.arraycopy(enc, 4, res, 32-rLength, rLength);
            rLength=32;
        } else if (rLength == 33 && enc[4] == 0) {
            rLength--;
            System.arraycopy(enc, 5, res, 0, rLength);
        } else {
            Log.e("cardCrypto","r-length:" + String.valueOf(rLength));
            Log.e("cardCrypto","s-length:" + String.valueOf(sLength));
            Log.e("cardCrypto","enc:" + Util.bytesToHex(enc));
            throw new Exception("unsupported r-length - r-length:" + String.valueOf(rLength)+",s-length:" + String.valueOf(sLength)+",enc:" +Util.bytesToHex(enc));
        }
        if (sLength <= 32) {
            System.arraycopy(enc, sPos, res, rLength+32-sLength, sLength);
            sLength=32;
        } else if (sLength == 33 && enc[sPos] == 0) {
            System.arraycopy(enc, sPos + 1, res, rLength, sLength - 1);
        } else {
            Log.e("cardCrypto","s-length:" + String.valueOf(sLength));
            Log.e("cardCrypto","r-length:" + String.valueOf(rLength));
            Log.e("cardCrypto","enc:" +Util.bytesToHex(enc));
            throw new Exception("unsupported s-length - r-length:" + String.valueOf(rLength)+",s-length:" + String.valueOf(sLength)+",enc:" +Util.bytesToHex(enc));
        }
        
        if(!VerifySignature(GeneratePublicKey(privateKeyArray), data, res))
        {
        	throw new Exception("Signature self verify failed - r-length:" + String.valueOf(rLength)+",s-length:" + String.valueOf(sLength)+",enc:" +Util.bytesToHex(enc)+",res:"+Util.bytesToHex(res));
        }
        
        return res;
    }

    public static byte[] GeneratePublicKey(byte[] privateKeyArray) throws NoSuchProviderException, NoSuchAlgorithmException {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");


        byte[] publicKeyArray = spec.getG().multiply(new BigInteger(1,privateKeyArray)).getEncoded(false);

        return publicKeyArray;
    }

    /**
     *  Computes the PBKDF2 hash of a password.
     *
     * @param   password    the password to hash.
     * @param   salt        the salt
     * @param   iterations  the iteration count (slowness factor)
     * @return              the PBDKF2 hash of the password
     */
    public static byte[] pbkdf2(byte[] password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        return PBKDF2.deriveKey(password, salt, iterations);
    }

    public static byte[] Encrypt(byte[] key, byte[] data) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/PKCS7PADDING");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
        byte[] mEncryptedData = cipher.doFinal(data);
        return mEncryptedData;
    }

    public static byte[] Decrypt(byte[] key, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/PKCS7PADDING");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] decryptedData = cipher.doFinal(Arrays.copyOfRange(data, 0, data.length ));
            return decryptedData;
        }
        catch (Exception e)
        {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/NOPADDING");
            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] decryptedData = cipher.doFinal(Arrays.copyOfRange(data, 0, data.length ));
            Log.e("decrypt",Util.bytesToHex(decryptedData));
            throw e;
        }
    }
}
