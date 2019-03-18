package com.ripple.crypto.ecdsa;

import com.ripple.crypto.keys.IVerifyingKey;
import com.ripple.encodings.addresses.Addresses;
import com.ripple.utils.HashUtils;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;

public class K256VerifyingKey implements IVerifyingKey {
    private final ECPublicKeyParameters keyParameters;
    private final byte[] canonicalPublicKey;

    K256VerifyingKey(ECPoint publicKey, byte[] publicKeyBytes) {
        if (publicKeyBytes == null) {
            publicKeyBytes = publicKey.getEncoded(true);
        } else if (publicKey == null) {
            publicKey = SECP256K1.curve().decodePoint(publicKeyBytes);
        }
        canonicalPublicKey = publicKeyBytes;
        keyParameters = new ECPublicKeyParameters(
                publicKey, SECP256K1.params());
    }

    private K256VerifyingKey(byte[] pubKeyBytes) {
        this(null, pubKeyBytes);
    }

    @Override
    public byte[] canonicalPubBytes() {
        return canonicalPublicKey;
    }

    @Override
    public boolean verify(byte[] message, byte[] signature) {
        byte[] bytes = HashUtils.halfSha512(message);
        return verifyHash(bytes, signature);
    }

    public boolean verifyHash(byte[] hash, byte[] signature) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(false, keyParameters);
        ECDSASignature sig = ECDSASignature.decodeFromDER(signature);
        return sig != null && signer.verifySignature(hash, sig.r, sig.s);
    }

    public static K256VerifyingKey fromNodePublicKey(String node) {
        byte[] pub = Addresses.decodeNodePublic(node);
        return new K256VerifyingKey(pub);
    }

    public static K256VerifyingKey fromCanonicalPubBytes(byte[] bytes) {
        return new K256VerifyingKey(bytes);
    }
}
