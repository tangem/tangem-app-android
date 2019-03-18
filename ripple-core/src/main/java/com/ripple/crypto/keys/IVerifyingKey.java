package com.ripple.crypto.keys;


import com.ripple.crypto.ecdsa.K256VerifyingKey;
import com.ripple.crypto.ed25519.EDVerifyingKey;
import com.ripple.encodings.common.B16;
import com.ripple.utils.HashUtils;

public interface IVerifyingKey {
    default String canonicalPubHex() {
        return B16.encode(canonicalPubBytes());
    }

    default byte[] id() {
        return HashUtils.SHA256_RIPEMD160(canonicalPubBytes());
    }

    byte[] canonicalPubBytes();

    boolean verify(byte[] message, byte[] sigBytes);

    static IVerifyingKey from(byte[] bytes) {
        if (bytes[0] == (byte) 0xED) {
            return EDVerifyingKey.fromCanonicalPubBytes(bytes);
        } else {
            return K256VerifyingKey.fromCanonicalPubBytes(bytes);
        }
    }
}