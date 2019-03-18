package com.ripple.crypto.ed25519;

import com.ripple.crypto.keys.IVerifyingKey;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.bouncycastle.util.Arrays;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EDVerifyingKey implements IVerifyingKey {
    private final EdDSAPublicKey publicKey;
    private final byte[] canonicalPubBytes;
    @SuppressWarnings("FieldCanBeLocal")
    private final byte[] ED_PREFIX = {(byte) 0xED};

    EDVerifyingKey(EdDSAPublicKeySpec spec, byte[] pubBytes) {
        if (pubBytes == null) {
            pubBytes = spec.getA().toByteArray();
        } else if (spec == null) {
            spec = new EdDSAPublicKeySpec(pubBytes, ED25519.ed25519);
        }
        publicKey = new EdDSAPublicKey(spec);
        canonicalPubBytes = Arrays.concatenate(ED_PREFIX, pubBytes);
    }

    MessageDigest sha512digest()  {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] canonicalPubBytes() {
        return canonicalPubBytes;
    }

    @Override
    public boolean verify(byte[] message, byte[] sigBytes) {
        try {
            EdDSAEngine sgr = new EdDSAEngine(sha512digest());
            sgr.initVerify(publicKey);
            sgr.update(message);
            return sgr.verify(sigBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static EDVerifyingKey fromCanonicalPubBytes(byte[] bytes) {
        // Strip the 0xED prefix from the key
        byte[] pubBytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        return new EDVerifyingKey(null, pubBytes);
    }
}
