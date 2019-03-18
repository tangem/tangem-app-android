package com.ripple.crypto;

import com.ripple.crypto.ecdsa.K256;
import com.ripple.crypto.ed25519.EDKeyPair;
import com.ripple.crypto.keys.IKeyPair;
import com.ripple.encodings.addresses.Addresses;
import com.ripple.encodings.base58.B58;
import com.ripple.utils.Sha512;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class Seed {
    private final byte[] seedBytes;
    private B58.Version version;

    public Seed(byte[] seedBytes) {
        this(Addresses.SEED_K256, seedBytes);
    }

    public Seed(B58.Version version, byte[] seedBytes) {
        this.seedBytes = seedBytes;
        this.version = version;
    }

    @Override
    public String toString() {
        return Addresses.encode(seedBytes, version);
    }

    public byte[] bytes() {
        return seedBytes;
    }

    public B58.Version version() {
        return version;
    }

    public Seed setEd25519() {
        this.version = Addresses.SEED_ED25519;
        return this;
    }

    public IKeyPair keyPair() {
        return keyPair(0);
    }

    public IKeyPair rootKeyPair() {
        return keyPair(-1);
    }

    public IKeyPair keyPair(int account) {
        if (version == Addresses.SEED_ED25519 ||
                Arrays.equals(version.bytes, Addresses.SEED_ED25519.bytes)) {
            if (account != 0) throw new IllegalStateException();
            return EDKeyPair.from128Seed(seedBytes);
        } else {
            return K256.createKeyPair(seedBytes, account);
        }

    }

    public static Seed fromBase58(String b58) {
        B58.Decoded decoded = Addresses.decodeSeed(b58);
        return new Seed(decoded.version, decoded.payload);
    }

    public static Seed fromPassPhrase(String passPhrase) {
        return new Seed(passPhraseToSeedBytes(passPhrase));
    }

    public static byte[] passPhraseToSeedBytes(String phrase) {
        try {
            return new Sha512(phrase.getBytes("utf-8")).finish128();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static IKeyPair getKeyPair(String b58) {
        return fromBase58(b58).keyPair();
    }
}


