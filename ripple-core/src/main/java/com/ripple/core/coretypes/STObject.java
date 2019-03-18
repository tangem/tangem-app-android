package com.ripple.core.coretypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ripple.core.coretypes.hash.HalfSha512;
import com.ripple.core.coretypes.hash.Hash128;
import com.ripple.core.coretypes.hash.Hash160;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.hash.prefixes.HashPrefix;
import com.ripple.core.coretypes.uint.UInt16;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.coretypes.uint.UInt8;
import com.ripple.core.fields.*;
import com.ripple.core.formats.Format;
import com.ripple.core.formats.LEFormat;
import com.ripple.core.formats.TxFormat;
import com.ripple.core.serialized.*;
import com.ripple.core.serialized.enums.EngineResult;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.serialized.enums.TransactionType;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.function.Predicate;

public class STObject implements SerializedType, Iterable<Field> {
    // Internally the fields are stored in a TreeMap
    public static class FieldsMap extends TreeMap<Field, SerializedType> {}
//    public static class FieldsMap extends HashMap<Field, SerializedType> {}

    protected FieldsMap fields;
    public Format format;

    public STObject() {
        fields = new FieldsMap();
    }
    public STObject(FieldsMap fieldsMap) {
        fields = fieldsMap;
    }

    public static STObject fromJSON(String json) {
        return fromJSONObject(new JSONObject(json));
    }
    public static STObject fromJSONObject(JSONObject json) {
        return translate.fromJSONObject(json);
    }
    public static STObject fromJacksonObject(ObjectNode object) {
        return translate.fromJacksonObject(object);
    }
    public static STObject fromHex(String hex) {
        return STObject.translate.fromHex(hex);
    }
    public static STObject fromBytes(byte[] bytes) {
        return translate.fromBytes(bytes);
    }
    public static STObject fromParser(BinaryParser parser) {
        return translate.fromParser(parser);
    }
    public static STObject fromParser(BinaryParser parser, Integer hint) {
        return translate.fromParser(parser, hint);
    }

    @Override
    public Iterator<Field> iterator() {
//        return fields.keySet().stream().sorted().iterator();
        return fields.keySet().iterator();
    }

    public String prettyJSON() {
        return translate.toJSONObject(this).toString(2);
    }

    /**
     * @return a subclass of STObject using the same fields
     *
     * If the object has a TransactionType or LedgerEntryType
     * then we can up)grade to a child class, with more specific
     * helper methods, and we can use `instanceof` to great effect.
     */
    public static STObject formatted(STObject source) {
        return STObjectFormatter.format(source);

    }

    public Format getFormat() {
        if (format == null) computeFormat();
        return format;
    }

    public static class FormatException extends RuntimeException {
        FormatException(String s) {
            super(s);
        }
    }

    public void checkFormat() {
        Format<?> fmt = getFormat();
        EnumMap<Field, Format.Requirement> requirements = fmt.requirements();
        for (Field field : this) {
            if (!requirements.containsKey(field)) {
                throw new FormatException(fmt.name() +
                        " doesn't have field: " + field);
            }
        }
        for (Field field : requirements.keySet()) {
            Format.Requirement req = requirements.get(field);
            if (!has(field)) {
                if (req == Format.Requirement.REQUIRED) {
                    throw new FormatException(fmt.name() +
                            " requires " + field + " of type "
                            + field.getType());
                }
            } else {
                SerializedType type = get(field);
                if (type.type() != field.getType()) {
                    if (!(field.getType() == Type.Hash160 &&
                            type.type() == Type.AccountID)) {
                        throw new FormatException(type.toString() +
                                " is not " + field.getType());
                    }
                }
            }
        }
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    private void computeFormat() {
        UInt16 tt = get(UInt16.TransactionType);
        if (tt != null) {
            setFormat(TxFormat.fromNumber(tt));
        }
        UInt16 let = get(UInt16.LedgerEntryType);
        if (let != null) {
            setFormat(LEFormat.fromNumber(let));
        }
    }

    public FieldsMap getFields() {
        return fields;
    }

    public SerializedType get(Field field) {
        return fields.get(field);
    }

    protected Hash256 signingHash(HashPrefix txSign) {
        HalfSha512 signing = HalfSha512.prefixed256(txSign);
        toBytesSink(signing, Field::isSigningField);
        return signing.finish();
    }

    protected byte[] signingData(HashPrefix txSign) {
        BytesList bl = new BytesList();
        bl.add(txSign.bytes());
        toBytesSink(bl, Field::isSigningField);
        return bl.bytes();
    }

    protected static EngineResult engineResult(STObject obj) {
        return (EngineResult) obj.get(Field.TransactionResult);
    }

    static public LedgerEntryType ledgerEntryType(STObject obj) {
        return (LedgerEntryType) obj.get(Field.LedgerEntryType);
    }

    public static TransactionType transactionType(STObject obj) {
        return (TransactionType) obj.get(Field.TransactionType);
    }

    public SerializedType remove(Field f) {
        return fields.remove(f);
    }

    public boolean has(Field f) {
        return fields.containsKey(f);
    }

    public <T extends HasField> boolean has(T hf) {
        return has(hf.getField());
    }

    public void put (UInt8Field f, UInt8 o) {put(f.getField(), o);}
    public void put (Vector256Field f, Vector256 o) {put(f.getField(), o);}
    public void put (BlobField f, Blob o) {put(f.getField(), o);}
    public void put (UInt64Field f, UInt64 o) {put(f.getField(), o);}
    public void put (UInt32Field f, UInt32 o) {put(f.getField(), o);}
    public void put (UInt16Field f, UInt16 o) {put(f.getField(), o);}
    public void put (PathSetField f, PathSet o) {put(f.getField(), o);}
    public void put (STObjectField f, STObject o) {put(f.getField(), o);}
    public void put (Hash256Field f, Hash256 o) {put(f.getField(), o);}
    public void put (Hash160Field f, Hash160 o) {put(f.getField(), o);}
    public void put (Hash128Field f, Hash128 o) {put(f.getField(), o);}
    public void put (STArrayField f, STArray o) {put(f.getField(), o);}
    public void put (AmountField f, Amount o) {put(f.getField(), o);}
    public void put (AccountIDField f, AccountID o) {put(f.getField(), o);}

    public <T extends HasField> void putTranslated(T f, Object value) {
        putTranslated(f.getField(), value);
    }

    public <T extends HasField> STObject as(T f, Object value) {
        putTranslated(f.getField(), value);
        return this;
    }

    public void put(Field f, SerializedType value) {
        fields.put(f, value);
    }

    public void putTranslated(Field f, Object value) {
        TypeTranslator typeTranslator = Translators.forField(f);
        SerializedType st;
        try {
            st = typeTranslator.fromValue(value);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't put `" +value+ "` into field `" + f + "`", e);
        }
        fields.put(f, st);
    }

    public AccountID get(AccountIDField f) {
        return (AccountID) get(f.getField());
    }

    public Amount get(AmountField f) {
        return (Amount) get(f.getField());
    }

    public STArray get(STArrayField f) {
        return (STArray) get(f.getField());
    }

    public Hash128 get(Hash128Field f) {
        return (Hash128) get(f.getField());
    }

    public Hash160 get(Hash160Field f) {
        return (Hash160) get(f.getField());
    }

    public Hash256 get(Hash256Field f) {
        return (Hash256) get(f.getField());
    }

    public STObject get(STObjectField f) {
        return (STObject) get(f.getField());
    }

    public PathSet get(PathSetField f) {
        return (PathSet) get(f.getField());
    }

    public UInt16 get(UInt16Field f) {
        return (UInt16) get(f.getField());
    }

    public UInt32 get(UInt32Field f) {
        return (UInt32) get(f.getField());
    }

    public UInt64 get(UInt64Field f) {
        return (UInt64) get(f.getField());
    }

    public UInt8 get(UInt8Field f) {
        return (UInt8) get(f.getField());
    }

    public Vector256 get(Vector256Field f) {
        return (Vector256) get(f.getField());
    }

    public Blob get(BlobField f) {
        return (Blob) get(f.getField());
    }

    // SerializedTypes implementation
    @Override
    public Object toJSON() {
        return translate.toJSON(this);
    }

    public JSONObject toJSONObject() {
        return translate.toJSONObject(this);
    }

    public byte[] toBytes() {
        return translate.toBytes(this);
    }

    @Override
    public String toHex() {
        return translate.toHex(this);
    }

    public void toBytesSink(BytesSink to, Predicate<Field> p) {
        BinarySerializer serializer = new BinarySerializer(to);

        for (Field field : this) {
            if (p.test(field)) {
                SerializedType value = fields.get(field);
                serializer.add(field, value);
            }
        }
    }
    @Override
    public void toBytesSink(BytesSink to) {
        toBytesSink(to, field -> field.isSerialized());
    }

    @Override
    public Type type() {
        return Type.STObject;
    }

    private static class Translator extends TypeTranslator<STObject> {

        @Override
        public STObject fromParser(BinaryParser parser, Integer hint) {
            STObject so = new STObject();
            TypeTranslator<SerializedType> tr;
            SerializedType st;
            Field field;
            Integer sizeHint;

            // hint, is how many bytes to parse
            if (hint != null) {
                // end hint
                hint = parser.pos() + hint;
            }

            while (!parser.end(hint)) {
                field = parser.readField();
                if (field == Field.ObjectEndMarker) {
                    break;
                }
                tr = Translators.forField(field);
                sizeHint = field.isVLEncoded() ? parser.readVLLength() : null;
                st = tr.fromParser(parser, sizeHint);
                if (st == null) {
                    throw new IllegalStateException("Parsed " + field + " as null");
                }
                so.put(field, st);
            }

            return STObject.formatted(so);
        }

        @Override
        public Object toJSON(STObject obj) {
            return toJSONObject(obj);
        }

        public JSONObject toJSONObject(STObject obj) {
            JSONObject json = new JSONObject();

            for (Field f : obj) {
                SerializedType obj1 = obj.get(f);
                Object object = obj1.toJSON();
                json.put(f.name(), object);
            }

            return json;
        }

        @Override
        public STObject fromJSONObject(JSONObject jsonObject) {
            STObject so = new STObject();

            Iterator keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object value   = jsonObject.get(key);
                Field fieldKey = Field.fromString(key);
                if (fieldKey == null) {
                    continue;
                }
                so.putTranslated(fieldKey, value);
            }
            return STObject.formatted(so);
        }

        @Override
        public STObject fromJacksonObject(ObjectNode object) {
            STObject so = new STObject();

            Iterator<String> keys = object.fieldNames();
            while (keys.hasNext()) {
                String key =  keys.next();
                JsonNode value  = object.get(key);
                Field fieldKey = Field.fromString(key);
                if (fieldKey == null) {
                    continue;
                }
                so.putTranslated(fieldKey, value);
            }
            return STObject.formatted(so);
        }
    }

    public int size() {
        return fields.size();
    }

    static private Translator translate = new Translator();

    private static STObjectField stobjectField(final Field f) {
        return new STObjectField() {@Override public Field getField() {return f; } };
    }

    static public STObjectField TransactionMetaData = stobjectField(Field.TransactionMetaData);
    static public STObjectField CreatedNode = stobjectField(Field.CreatedNode);
    static public STObjectField DeletedNode = stobjectField(Field.DeletedNode);
    static public STObjectField ModifiedNode = stobjectField(Field.ModifiedNode);
    static public STObjectField PreviousFields = stobjectField(Field.PreviousFields);
    static public STObjectField FinalFields = stobjectField(Field.FinalFields);
    static public STObjectField NewFields = stobjectField(Field.NewFields);
    static public STObjectField TemplateEntry = stobjectField(Field.TemplateEntry);
    static public STObjectField Signer = stobjectField(Field.Signer);
    static public STObjectField SignerEntry = stobjectField(Field.SignerEntry);
    static public STObjectField ObjectEndMarker = stobjectField(Field.ObjectEndMarker);
    static public STObjectField Memo = stobjectField(Field.Memo);
    static public STObjectField Majority = stobjectField(Field.Majority);

    private static class Translators {
        @SuppressWarnings("unused")
        private static TypeTranslator get(Class<? extends SerializedType> kls) {
            try {
                java.lang.reflect.Field translate = kls.getDeclaredField("translate");
                translate.setAccessible(true);
                return (TypeTranslator) translate.get(kls);
            } catch (Exception e) {
                throw new RuntimeException("for kls: " + kls.getSimpleName(), e);
            }
        }

        private static TypeTranslator forType(Type type) {
            switch (type) {
                case STObject:      return STObject.translate; // get(STObject.class);
                case Amount:        return Amount.translate; // get(Amount.class);
                case UInt16:        return UInt16.translate; // get(UInt16.class);
                case UInt32:        return UInt32.translate; // get(UInt32.class);
                case UInt64:        return UInt64.translate; // get(UInt64.class);
                case Hash128:       return Hash128.translate; // get(Hash128.class);
                case Hash256:       return Hash256.translate; // get(Hash256.class);
                case Blob:          return Blob.translate; // get(Blob.class);
                case AccountID:     return AccountID.translate; // get(AccountID.class);
                case STArray:       return STArray.translate; // get(STArray.class);
                case UInt8:         return UInt8.translate; // get(UInt8.class);
                case Hash160:       return Hash160.translate; // get(Hash160.class);
                case PathSet:       return PathSet.translate; // get(PathSet.class);
                case Vector256:     return Vector256.translate; // get(Vector256.class);
                default:            throw new IllegalStateException("Unknown type");
            }
        }

        private static TypeTranslator<SerializedType> forField(Field field) {
            if (field.tag == null) {
                switch (field) {
                    case LedgerEntryType:
                        field.tag = LedgerEntryType.translate; //get(LedgerEntryType.class);
                        break;
                    case TransactionType:
                        field.tag = TransactionType.translate;// get(TransactionType.class);
                        break;
                    case TransactionResult:
                        field.tag = EngineResult.translate; // get(EngineResult.class);
                        break;
                    default:
                        field.tag = forType(field.getType());
                        break;
                }
            }
            return getCastedTag(field);
        }

        @SuppressWarnings("unchecked")
        private static TypeTranslator<SerializedType> getCastedTag(Field field) {
            return (TypeTranslator<SerializedType>) field.tag;
        }
    }
}
