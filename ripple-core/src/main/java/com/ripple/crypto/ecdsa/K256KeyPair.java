package com.ripple.crypto.ecdsa;

import com.ripple.crypto.keys.IKeyPair;
import com.ripple.utils.HashUtils;
import com.ripple.utils.Utils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

public class K256KeyPair extends K256VerifyingKey implements IKeyPair {
    private byte[] privateKey;
    private ECPrivateKeyParameters privateKeyParameters;

    K256KeyPair(BigInteger privateKey, ECPoint pub, byte[] pubEncoded) {
        super(pub, pubEncoded);
        this.privateKey = Utils.padTo256(privateKey.toByteArray());
        privateKeyParameters = new ECPrivateKeyParameters(privateKey,
                SECP256K1.params());
    }

    @Override
    public byte[] privateKey() {
        return privateKey;
    }

    @Override
    public byte[] signMessage(byte[] message) {
        byte[] hash = HashUtils.halfSha512(message);
        return signHash(hash);
    }

    public byte[] signHash(byte[] bytes) {
        ECDSASigner signer = newSigner();

        ECDSASignature sig = K256.createECDSASignature(bytes, signer);
        byte[] der = sig.encodeToDER();
        if (!ECDSASignature.isStrictlyCanonical(der)) {
            throw new IllegalStateException("Signature is not strictly canonical");
        }
        return der;
    }

    private ECDSASigner newSigner() {
        ECDSASigner signer = new ECDSASigner(
                new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, privateKeyParameters);
        return signer;
    }

}
