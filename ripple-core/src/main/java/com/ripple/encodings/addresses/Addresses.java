package com.ripple.encodings.addresses;


import com.ripple.encodings.base58.B58;
import com.ripple.encodings.basex.EncodingFormatException;
import com.ripple.encodings.basex.IBaseX;
import org.omg.IOP.Encoding;

public class Addresses {
    public static final B58 codec = new B58("rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz");

    // Otherwise known as `family seed`
    public static final B58.Version SEED_K256 = new B58.Version(
            33, "seedK256", 16);

    public static final B58.Version SEED_ED25519 = new B58.Version(
            "01E14B", "seedEd25519", 16);

    public static final B58.Version ACCOUNT_ID = new B58.Version(
            0, "accountId", 20);

    public static final B58.Version NODE_PUBLIC_KEY = new B58.Version(
            28, "nodePublicKey", 33);

    public static final B58.Version NODE_PRIVATE_KEY = new B58.Version(
            33, "nodePrivateKey", 32);

    public static byte[] decode(String encoded, B58.Version version) {
        return codec.decodeVersioned(encoded, version).payload;
    }

    public static String encode(byte[] bytes, B58.Version version) {
        return codec.encodeVersioned(bytes, version);
    }

    public static byte[] decodeSeedToBytes(String seed) {
        return codec.decodeVersioned(seed, SEED_K256, SEED_ED25519)
                .payload;
    }

    public static B58.Decoded decodeSeed(String seed) {
        return codec.decodeVersioned(seed, SEED_K256, SEED_ED25519);
    }

    public static String encodeSeedK256(byte[] bytes) {
        return encode(bytes, SEED_K256);
    }

    public static String encodeAccountID(byte[] bytes) {
        return encode(bytes, ACCOUNT_ID);
    }

    public static byte[] decodeAccountID(String id) {
        return decode(id, ACCOUNT_ID);
    }

    public static String encodeNodePublic(byte[] bytes) {
        return encode(bytes, NODE_PUBLIC_KEY);
    }

    public static byte[] decodeNodePublic(String base58) {
        return decode(base58, NODE_PUBLIC_KEY);
    }

    public static boolean isValid(String encoded, B58.Version... versions) {
        try {
            codec.decodeVersioned(encoded, versions);
            return true;
        } catch (EncodingFormatException e) {
            return false;
        }
    }

    public static boolean isValidAccountID(String encoded) {
        return isValid(encoded, ACCOUNT_ID);
    }
}
