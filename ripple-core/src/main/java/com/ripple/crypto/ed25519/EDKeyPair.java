package com.ripple.crypto.ed25519;

import com.ripple.crypto.keys.IKeyPair;
import com.ripple.utils.HashUtils;
import com.ripple.utils.Utils;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.math.BigInteger;
import java.security.PrivateKey;

public class EDKeyPair extends EDVerifyingKey implements IKeyPair {

    private final EdDSAPrivateKeySpec keySpec;

    private EDKeyPair(EdDSAPrivateKeySpec keySpec) {
        super(new EdDSAPublicKeySpec(keySpec.getA(), ED25519.ed25519), null);
        this.keySpec = keySpec;
    }

    public static EDKeyPair from256Seed(byte[] seedBytes) {
        EdDSAPrivateKeySpec keySpec = new EdDSAPrivateKeySpec(seedBytes,
                                                              ED25519.ed25519);
        return new EDKeyPair(keySpec);
    }

    public static EDKeyPair from128Seed(byte[] seedBytes) {
        assert seedBytes.length == 16;
        return from256Seed(HashUtils.halfSha512(seedBytes));
    }

    @Override
    public byte[] privateKey() {
        return keySpec.geta();
    }

    @Override
    public byte[] signMessage(byte[] message) {
        try {
            EdDSAEngine sgr = new EdDSAEngine(sha512digest());
            PrivateKey sKey = new EdDSAPrivateKey(keySpec);
            sgr.initSign(sKey);
            sgr.update(message);
            return sgr.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
