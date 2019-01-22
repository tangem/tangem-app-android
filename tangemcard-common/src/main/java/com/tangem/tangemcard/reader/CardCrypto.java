package com.tangem.tangemcard.reader;

import com.tangem.tangemcard.util.Log;
import com.tangem.tangemcard.util.PBKDF2;
import com.tangem.tangemcard.util.Util;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
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
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        Security.addProvider(new EdDSASecurityProvider());
    }

    public enum Curve {secp256k1, ed25519}

    public static PublicKey LoadPublicKey(Curve curve, byte[] publicKeyArray) throws Exception {
        if (publicKeyArray == null) throw new Exception("Public key not specified!");
        switch (curve) {
            case secp256k1: {
                ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
                KeyFactory factory = KeyFactory.getInstance("EC", "SC");

                ECPoint p1 = spec.getCurve().decodePoint(publicKeyArray);
                ECPublicKeySpec keySpec = new ECPublicKeySpec(p1, spec);

                return factory.generatePublic(keySpec);
            }
            case ed25519: {
                EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
                EdDSAPublicKeySpec pubKey = new EdDSAPublicKeySpec(publicKeyArray, spec);
                return new EdDSAPublicKey(pubKey);
            }

            default:
                throw new Exception(curve.toString() + " not supported");
        }
    }

    public static PublicKey LoadPublicKey(byte[] publicKeyArray) throws Exception {
        return LoadPublicKey(Curve.secp256k1, publicKeyArray);
    }

    public static boolean VerifySignature(Curve curve, byte[] publicKeyArray, byte[] data, byte[] signature) throws Exception {
        switch (curve) {
            case secp256k1: {
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
            case ed25519: {
                data = Util.calculateSHA512(data);
                PublicKey publicKey = LoadPublicKey(curve, publicKeyArray);
                EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
                Signature signatureInstance = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
                signatureInstance.initVerify(publicKey);

                signatureInstance.update(data);

                return signatureInstance.verify(signature);
            }
            default:
                throw new Exception(curve.toString() + " not supported");
        }
    }

    public static boolean VerifySignature(byte[] publicKeyArray, byte[] data, byte[] signature) throws Exception {
        return VerifySignature(Curve.secp256k1, publicKeyArray, data, signature);
    }

    public static boolean VerifySignature(String curveID, byte[] publicKeyArray, byte[] data, byte[] signature) throws Exception {
        Curve curve;
        try {
            curve = Curve.valueOf(curveID);
        } catch (Exception e) {
            throw new Exception("Card EC curve (" + curveID + ") isn't supported!");
        }
        return VerifySignature(curve, publicKeyArray, data, signature);
    }


    public static byte[] Signature(Curve curve, byte[] privateKeyArray, byte[] data) throws Exception {
        switch (curve) {
            case secp256k1: {
                ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
                KeyFactory factory = KeyFactory.getInstance("EC", "SC");

                ECPrivateKeySpec keySpecP = new ECPrivateKeySpec(new BigInteger(1, privateKeyArray), spec);

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
                    System.arraycopy(enc, 4, res, 32 - rLength, rLength);
                    rLength = 32;
                } else if (rLength == 33 && enc[4] == 0) {
                    rLength--;
                    System.arraycopy(enc, 5, res, 0, rLength);
                } else {
                    Log.e("cardCrypto", "r-length:" + String.valueOf(rLength));
                    Log.e("cardCrypto", "s-length:" + String.valueOf(sLength));
                    Log.e("cardCrypto", "enc:" + Util.bytesToHex(enc));
                    throw new Exception("unsupported r-length - r-length:" + String.valueOf(rLength) + ",s-length:" + String.valueOf(sLength) + ",enc:" + Util.bytesToHex(enc));
                }
                if (sLength <= 32) {
                    System.arraycopy(enc, sPos, res, rLength + 32 - sLength, sLength);
                    sLength = 32;
                } else if (sLength == 33 && enc[sPos] == 0) {
                    System.arraycopy(enc, sPos + 1, res, rLength, sLength - 1);
                } else {
                    Log.e("cardCrypto", "s-length:" + String.valueOf(sLength));
                    Log.e("cardCrypto", "r-length:" + String.valueOf(rLength));
                    Log.e("cardCrypto", "enc:" + Util.bytesToHex(enc));
                    throw new Exception("unsupported s-length - r-length:" + String.valueOf(rLength) + ",s-length:" + String.valueOf(sLength) + ",enc:" + Util.bytesToHex(enc));
                }

                if (!VerifySignature(GeneratePublicKey(privateKeyArray), data, res)) {
                    throw new Exception("Signature self verify failed - r-length:" + String.valueOf(rLength) + ",s-length:" + String.valueOf(sLength) + ",enc:" + Util.bytesToHex(enc) + ",res:" + Util.bytesToHex(res));
                }

                return res;
            }
            case ed25519: {
                data = Util.calculateSHA512(data);
                EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
                //Signature sgr = Signature.getInstance("EdDSA", "I2P");
                Signature signatureInstance = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));

                EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(privateKeyArray, spec);
                PrivateKey privateKey = new EdDSAPrivateKey(privateKeySpec);

                signatureInstance.initSign(privateKey);
                signatureInstance.update(data);

                return signatureInstance.sign();
            }
            default:
                throw new Exception(curve.toString() + " not supported");
        }
    }

    public static byte[] Signature(byte[] privateKeyArray, byte[] data) throws Exception {
        return Signature(Curve.secp256k1, privateKeyArray, data);
    }

    public static byte[] GeneratePublicKey(Curve curve, byte[] privateKeyArray) throws Exception {
        switch (curve) {
            case secp256k1: {
                ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");


                byte[] publicKeyArray = spec.getG().multiply(new BigInteger(1, privateKeyArray)).getEncoded(false);

                return publicKeyArray;
            }
            case ed25519: {
                EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);

                EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(privateKeyArray, spec);
                EdDSAPublicKeySpec publicKeySpec = new EdDSAPublicKeySpec(privateKeySpec.getA(), spec);
                EdDSAPublicKey publicKey = new EdDSAPublicKey(publicKeySpec);
                return publicKey.getAbyte();
            }
            default:
                throw new Exception(curve.toString() + " not supported");
        }
    }


    public static byte[] GeneratePublicKey(byte[] privateKeyArray) throws Exception {
        return GeneratePublicKey(Curve.secp256k1, privateKeyArray);
    }

    /**
     * Computes the PBKDF2 hash of a password.
     *
     * @param password   the password to hash.
     * @param salt       the salt
     * @param iterations the iteration count (slowness factor)
     * @return the PBDKF2 hash of the password
     */
    public static byte[] pbkdf2(byte[] password, byte[] salt, int iterations)
            throws InvalidKeyException {
        return PBKDF2.deriveKey(password, salt, iterations);
    }

    public static byte[] Encrypt(byte[] key, byte[] data, boolean UsePKCS7) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        if (UsePKCS7) {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/PKCS7PADDING");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING", "SC");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] mEncryptedData = cipher.doFinal(data);
            return mEncryptedData;
        } else {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/NOPADDING");
            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING", "SC");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] mEncryptedData = cipher.doFinal(data);
            return mEncryptedData;
        }
    }

    public static byte[] Encrypt(byte[] key, byte[] data) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return Encrypt(key, data, true);
    }

    public static byte[] Decrypt(byte[] key, byte[] data, boolean UsePKCS7)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        if (UsePKCS7) {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/PKCS7PADDING");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] decryptedData = cipher.doFinal(Arrays.copyOfRange(data, 0, data.length));
            return decryptedData;
        } else {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES/CBC/NOPADDING");
            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] decryptedData = cipher.doFinal(Arrays.copyOfRange(data, 0, data.length));
            return decryptedData;
        }
    }
}
