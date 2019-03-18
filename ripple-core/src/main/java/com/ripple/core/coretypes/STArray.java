package com.ripple.core.coretypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.STArrayField;
import com.ripple.core.fields.Type;
import com.ripple.core.serialized.BinaryParser;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.TypeTranslator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class STArray extends ArrayList<STObject> implements SerializedType {

    public JSONArray toJSONArray() {
        JSONArray array = new JSONArray();

        for (STObject so : this) {
            array.put(so.toJSON());
        }

        return array;
    }

    @Override
    public Object toJSON() {
        return toJSONArray();
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
        for (STObject stObject : this) {
            stObject.toBytesSink(to);
        }
    }

    @Override
    public Type type() {
        return Type.STArray;
    }

    public static STArray fromParser(BinaryParser parser) {
        return translate.fromParser(parser);
    }
    public static STArray fromBytes(byte[] bytes) {
        return translate.fromBytes(bytes);
    }
    public static STArray fromHex(String hex) {
        return translate.fromHex(hex);
    }

    public static class Translator extends TypeTranslator<STArray> {
        @Override
        public STArray fromParser(BinaryParser parser, Integer hint) {
            STArray stArray = new STArray();
            while (!parser.end()) {
                Field field = parser.readField();
                if (field == Field.ArrayEndMarker) {
                    break;
                }
                STObject outer = new STObject();
                // assert field.getType() == Type.STObject;
                outer.put(field, STObject.fromParser(parser));
                stArray.add(STObject.formatted(outer));
            }
            return stArray;
        }

        public JSONArray toJSONArray(STArray obj) {
            return obj.toJSONArray();
        }

        @Override
        public STArray fromJSONArray(JSONArray jsonArray) {
            STArray arr = new STArray();

            for (int i = 0; i < jsonArray.length(); i++) {
                Object o = jsonArray.get(i);
                arr.add(STObject.fromJSONObject((JSONObject) o));
            }

            return arr;
        }
        @Override
        public STArray fromJacksonArray(ArrayNode jsonArray) {
            STArray arr = new STArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                ObjectNode object = (ObjectNode) jsonArray.get(i);
                arr.add(STObject.fromJacksonObject(object));
            }

            return arr;
        }
    }
    static public Translator translate = new Translator();

    public STArray(){}

    private static STArrayField starrayField(final Field f) {
        return new STArrayField(){ @Override public Field getField() {return f;}};
    }

    static public STArrayField AffectedNodes = starrayField(Field.AffectedNodes);
    static public STArrayField SignerEntries = starrayField(Field.SignerEntries);
    static public STArrayField Signers = starrayField(Field.Signers);

    static public STArrayField Template = starrayField(Field.Template);
    static public STArrayField Necessary = starrayField(Field.Necessary);
    static public STArrayField Sufficient = starrayField(Field.Sufficient);
    static public STArrayField Majorities = starrayField(Field.Majorities);
    static public STArrayField Memos = starrayField(Field.Memos);
    static public STArrayField ArrayEndMarker = starrayField(Field.ArrayEndMarker);
}
