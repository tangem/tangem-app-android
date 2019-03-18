package com.ripple.crypto.ecdsa;

import com.ripple.utils.Sha512;
import com.ripple.utils.Utils;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

public class K256 {

    /**
     * @param secretKey secret point on the curve as BigInteger
     * @return corresponding public point
     */
    public static byte[] getPublic(BigInteger secretKey) {
        return SECP256K1.basePointMultipliedBy(secretKey);
    }

    /**
     * @param secretKey secret point on the curve as BigInteger
     * @return corresponding public point
     */
    private static ECPoint computePublic(BigInteger secretKey) {
        return SECP256K1.basePoint().multiply(secretKey);
    }

    /**
     * @param privateGen secret point on the curve as BigInteger
     * @return the corresponding public key is the public generator
     * (aka public root key, master public key).
     * return as byte[] for convenience.
     */
    private static ECPoint computePublicGenerator(BigInteger privateGen) {
        return computePublic(privateGen);
    }

    private static BigInteger computePrivateGen(byte[] seedBytes) {
        return generateKey(seedBytes, null);
    }

    public static byte[] computePublicKey(byte[] publicGenBytes,
                                          int accountNumber) {
        ECPoint rootPubPoint = SECP256K1.curve().decodePoint(publicGenBytes);
        BigInteger scalar = generateKey(publicGenBytes, accountNumber);
        ECPoint point = SECP256K1.basePoint().multiply(scalar);
        ECPoint offset = rootPubPoint.add(point);
        return offset.getEncoded(true);
    }

    private static BigInteger computeSecretKey(BigInteger privateGen,
                                               byte[] publicGenBytes,
                                               int accountNumber) {
        return generateKey(publicGenBytes, accountNumber)
                .add(privateGen).mod(SECP256K1.order());
    }

    /**
     * @param seedBytes     - a bytes sequence of arbitrary length which will be hashed
     * @param discriminator - nullable optional uint32 to hash
     * @return a number between [1, order -1] suitable as a private key
     */
    private static BigInteger generateKey(byte[] seedBytes, Integer discriminator) {
        BigInteger key = null;
        for (long i = 0; i <= 0xFFFFFFFFL; i++) {
            Sha512 sha512 = new Sha512().add(seedBytes);
            if (discriminator != null) {
                sha512.addU32(discriminator);
            }
            sha512.addU32((int) i);
            byte[] keyBytes = sha512.finish256();
            key = Utils.uBigInt(keyBytes);
            if (key.compareTo(BigInteger.ZERO) > 0 &&
                    key.compareTo(SECP256K1.order()) < 0) {
                break;
            }
        }
        return key;
    }

    static ECDSASignature createECDSASignature(byte[] hash, ECDSASigner signer) {
        return ECDSASignature.createSignature(hash, signer);
    }

    public static K256KeyPair createKeyPair(byte[] seedBytes, int accountNumber) {
        @SuppressWarnings("SpellCheckingInspection")
        BigInteger priv;
        BigInteger privateGen;
        // The private generator (aka root private key, master private key)
        privateGen = computePrivateGen(seedBytes);
        ECPoint publicGen = computePublicGenerator(privateGen);
        byte[] pubGenBytes = publicGen.getEncoded(true);

        if (accountNumber == -1) {
            // The root keyPair
            return new K256KeyPair(privateGen, publicGen, pubGenBytes);
        } else {
            priv = computeSecretKey(privateGen, pubGenBytes, accountNumber);
            ECPoint pub = computePublic(priv);
            return new K256KeyPair(priv, pub, null);
        }
    }
}
