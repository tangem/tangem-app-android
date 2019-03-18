
package com.ripple.core.coretypes;

import com.ripple.core.fields.BlobField;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.Type;
import com.ripple.core.serialized.BinaryParser;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.TypeTranslator;
import com.ripple.encodings.common.B16;
import org.bouncycastle.util.encoders.Hex;

public class Blob implements SerializedType {
    public Blob(byte[] bytes) {
        buffer = bytes.clone();
    }

    private final byte[] buffer;

    @Override
    public Object toJSON() {
        return translate.toJSON(this);
    }

    @Override
    public byte[] toBytes() {
        return buffer;
    }

    @Override
    public String toHex() {
        return translate.toHex(this);
    }

    @Override
    public void toBytesSink(BytesSink to) {
        translate.toBytesSink(this, to);
    }

    @Override
    public Type type() {
        return Type.Blob;
    }

    public static Blob fromBytes(byte[] bytes) {
        return new Blob(bytes);
    }

    public static Blob fromHex(String hex) {
        return fromBytes(B16.decode(hex));
    }

    public static Blob fromParser(BinaryParser parser, int hint) {
        return translate.fromParser(parser, hint);
    }
    public static Blob fromParser(BinaryParser parser) {
        return translate.fromParser(parser, null);
    }

    public static class Translator extends TypeTranslator<Blob> {
        @Override
        public Blob fromParser(BinaryParser parser, Integer hint) {
            if (hint == null) {
                hint = parser.size() - parser.pos();
            }
            return new Blob(parser.read(hint));
        }

        @Override
        public Object toJSON(Blob obj) {
            return toString(obj);
        }

        @Override
        public String toString(Blob obj) {
            return B16.encode(obj.buffer);
        }

        @Override
        public Blob fromString(String value) {
            return new Blob(Hex.decode(value));
        }

        @Override
        public void toBytesSink(Blob obj, BytesSink to) {
            to.add(obj.buffer.clone());
        }
    }

    static public Translator translate = new Translator();

    private static BlobField blobField(final Field f) {
        return new BlobField() {
            @Override
            public Field getField() {
                return f;
            }
        };
    }

    static public BlobField PublicKey = blobField(Field.PublicKey);
    static public BlobField MessageKey = blobField(Field.MessageKey);
    static public BlobField SigningPubKey = blobField(Field.SigningPubKey);
    static public BlobField TxnSignature = blobField(Field.TxnSignature);
    static public BlobField MasterSignature = blobField(Field.MasterSignature);
    static public BlobField Signature = blobField(Field.Signature);
    static public BlobField Domain = blobField(Field.Domain);
    static public BlobField FundCode = blobField(Field.FundCode);
    static public BlobField RemoveCode = blobField(Field.RemoveCode);
    static public BlobField ExpireCode = blobField(Field.ExpireCode);
    static public BlobField CreateCode = blobField(Field.CreateCode);

    static public BlobField MemoType = blobField(Field.MemoType);
    static public BlobField MemoData = blobField(Field.MemoData);
    static public BlobField MemoFormat = blobField(Field.MemoFormat);
    static public BlobField Condition = blobField(Field.Condition);
    static public BlobField Fulfillment = blobField(Field.Fulfillment);
}
