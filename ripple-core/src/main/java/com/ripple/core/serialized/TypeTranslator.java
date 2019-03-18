
package com.ripple.core.serialized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ripple.core.runtime.Value;
import com.ripple.encodings.common.B16;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @param <T> The SerializedType class
 * TODO, this should only really have methods that each class over-rides
 *       it's currently pretty NASTY
 */
public abstract class TypeTranslator<T extends SerializedType> {

    @SuppressWarnings("unchecked")
    public T fromValue(Object object) {
        switch (Value.typeOf(object)) {
            case STRING:
                return fromString((String) object);
            case JACKSON_ARRAY:
                return fromJacksonArray((ArrayNode) object);
            case JACKSON_BINARY:
                throw new IllegalStateException("cant handle jackson binary");
            case JACKSON_BOOLEAN:
                return fromBoolean(((JsonNode) object).asBoolean());
            case JACKSON_MISSING:
                throw new IllegalStateException("cant create from missing");
            case JACKSON_NULL:
                throw new IllegalStateException("cant create from null");
            case JACKSON_NUMBER:
                JsonNode node = (JsonNode) object;
                if (node.isLong()) {
                    return fromLong(node.asLong());
                } else if (node.isInt()) {
                    return fromInteger(node.asInt());
                } else if (node.isDouble()) {
                    return fromDouble(node.asDouble());
                } else if (node.isFloat()) {
                    return fromDouble(node.floatValue());
                }
                throw new IllegalStateException("cant create from null");
            case JACKSON_OBJECT:
                return fromJacksonObject((ObjectNode) object);
            case JACKSON_POJO:
                throw new IllegalStateException("cant handle POJO");
            case JACKSON_STRING:
                return fromString(((JsonNode) object).asText());
            case DOUBLE:
                return fromDouble((Double) object);
            case INTEGER:
                return fromInteger((Integer) object);
            case LONG:
                return fromLong((Long) object);
            case BOOLEAN:
                return fromBoolean((Boolean) object);
            case JSON_ARRAY:
                return fromJSONArray((JSONArray) object);
            case JSON_OBJECT:
                return fromJSONObject((JSONObject) object);
            case BYTE_ARRAY:
                return fromBytes((byte[]) object);
            case UNKNOWN:
            default:
                return (T) object;
        }

    }

    protected T fromJacksonObject(ObjectNode object) {
        throw new UnsupportedOperationException();
    }

    protected T fromJacksonArray(ArrayNode node) {
        throw new UnsupportedOperationException();
    }

    public String toString(T obj) {
        return obj.toString();
    }

    protected T fromJSONObject(JSONObject jsonObject) {
        throw new UnsupportedOperationException();
    }

    protected T fromJSONArray(JSONArray jsonArray) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("WeakerAccess")
    protected T fromBoolean(boolean aBoolean) {
        throw new UnsupportedOperationException();
    }

    protected T fromLong(long aLong) {
        throw new UnsupportedOperationException();
    }

    protected T fromInteger(int integer) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("WeakerAccess")
    protected T fromDouble(double aDouble) {
        throw new UnsupportedOperationException();
    }

    protected T fromString(String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param hint Using a boxed integer, allowing null for no hint
     */
    public abstract T fromParser(BinaryParser parser, Integer hint);

    public T fromParser(BinaryParser parser) {
        return fromParser(parser, null);
    }

    public T fromBytes(byte[] b) {
        return fromParser(new BinaryParser(b));
    }

    public T fromHex(String hex) {
        return fromBytes(B16.decode(hex));
    }

    public Object toJSON(T obj) {
        return obj.toJSON();
    }

    public void toBytesSink(T obj, BytesSink to) {
        obj.toBytesSink(to);
    }

    public byte[] toBytes(T obj) {
        BytesList to = new BytesList();
        toBytesSink(obj, to);
        return to.bytes();
    }

    public String toHex(T obj) {
        BytesList to = new BytesList();
        toBytesSink(obj, to);
        return to.bytesHex();
    }
}
