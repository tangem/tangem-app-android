package com.ripple.core.coretypes;

import com.ripple.core.coretypes.hash.Hash160;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.hash.Index;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.AccountIDField;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.Type;
import com.ripple.core.serialized.BinaryParser;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.TypeTranslator;
import com.ripple.crypto.Seed;
import com.ripple.crypto.keys.IKeyPair;
import com.ripple.encodings.addresses.Addresses;
import com.ripple.encodings.common.B16;
import com.ripple.utils.Utils;

/**
 * Originally it was intended that AccountIDs would be variable length so that's
 * why they are variable length encoded as top level field objects.
 *
 * Note however, that in practice, all account ids are just 160 bit hashes.
 * Consider the fields TakerPaysIssuer and fixed length encoding of issuers in
 * amount serializations.
 *
 * Thus, we extend Hash160 which affords us some functionality.
 */
public class AccountID extends Hash160 {
    public static final AccountID NEUTRAL = fromInteger(1);
    public static final AccountID XRP_ISSUER = fromInteger(0);

    public final String address;

    public AccountID(byte[] bytes) {
        this(bytes, encodeAddress(bytes));
    }

    public AccountID(byte[] bytes, String address) {
        super(bytes);
        this.address = address;
    }

    // Static from* constructors
    public static AccountID fromString(String value) {
        if (value.length() == 160 / 4) {
            return fromBytes(B16.decode(value));
        } else {
            return fromAddress(value);
        }
    }

    static public AccountID fromAddress(String address) {
        byte[] bytes = Addresses.decodeAccountID(address);
        return new AccountID(bytes, address);
    }

    static public AccountID fromParser(BinaryParser parser) {
        return translate.fromParser(parser);
    }

    static public AccountID fromHex(String hex) {
        return translate.fromHex(hex);
    }

    public static AccountID fromKeyPair(IKeyPair kp) {
        byte[] bytes = kp.id();
        return new AccountID(bytes, encodeAddress(bytes));
    }

    public static AccountID fromPassPhrase(String phrase) {
        return fromKeyPair(Seed.fromPassPhrase(phrase).keyPair());
    }

    static public AccountID fromSeed(String seed) {
        return fromKeyPair(Seed.getKeyPair(seed));
    }

    private static AccountID fromInteger(Integer n) {
        return fromBytes(Utils.padTo160(new UInt32(n).toByteArray()));
    }

    public static AccountID fromBytes(byte[] bytes) {
        return new AccountID(bytes, encodeAddress(bytes));
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return address;
    }

    public Issue issue(String code) {
        return new Issue(Currency.fromString(code), this);
    }

    public Issue issue(Currency c) {
        return new Issue(c, this);
    }

    public boolean isNativeIssuer() {
        return this == XRP_ISSUER || equals(XRP_ISSUER);
    }

    // SerializedType interface implementation
    @Override
    public Object toJSON() {
        return toString();
    }

    @Override
    public byte[] toBytes() {
        return translate.toBytes(this);
    }

    @Override
    public String toHex() {
        return translate.toHex(this);
    }

    @Override
    public void toBytesSink(BytesSink to) {
        to.add(bytes());
    }

    @Override
    public Type type() {
        return Type.AccountID;
    }

    public Hash256 lineIndex(Issue issue) {
        if (issue.isNative()) throw new AssertionError();
        return Index.rippleState(this, issue.issuer(), issue.currency());
    }

    public static class Translator extends TypeTranslator<AccountID> {
        @Override
        public AccountID fromParser(BinaryParser parser, Integer hint) {
            if (hint == null) {
                hint = 20;
            }
            return AccountID.fromBytes(parser.read(hint));
        }

        @Override
        public String toString(AccountID obj) {
            return obj.toString();
        }

        @Override
        public AccountID fromString(String value) {
            return AccountID.fromString(value);
        }
    }

    //

    static public Translator translate = new Translator();

    // helpers

    private static String encodeAddress(byte[] address) {
        return Addresses.encodeAccountID(address);
    }

    // Typed field definitions
    private static AccountIDField accountField(final Field f) {
        return new AccountIDField() {
            @Override
            public Field getField() {
                return f;
            }
        };
    }

    static public AccountIDField Account = accountField(Field.Account);
    static public AccountIDField Owner = accountField(Field.Owner);
    static public AccountIDField Destination = accountField(Field.Destination);
    static public AccountIDField Issuer = accountField(Field.Issuer);
    static public AccountIDField Target = accountField(Field.Target);
    static public AccountIDField RegularKey = accountField(Field.RegularKey);
    static public AccountIDField Authorize = accountField(Field.Authorize);
    static public AccountIDField Unauthorize = accountField(Field.Unauthorize);
}
