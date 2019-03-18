package com.ripple.core.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public enum Value {
    UNKNOWN,
    STRING,
    JSON_OBJECT,
    JSON_ARRAY,

    JACKSON_ARRAY,
    JACKSON_BINARY,
    JACKSON_BOOLEAN,
    JACKSON_MISSING,
    JACKSON_NULL,
    JACKSON_NUMBER,
    JACKSON_OBJECT,
    JACKSON_POJO,
    JACKSON_STRING,

    LIST,
    MAP,
    NUMBER,
    BYTE,
    DOUBLE,
    FLOAT,
    INTEGER,
    LONG,
    BYTE_ARRAY,
    SHORT,
    BOOLEAN;

    static public Value typeOf (Object object) {
        if (object instanceof String) {
            return STRING;
        }
        else if (object instanceof Number) {
            if (object instanceof Byte) {
                return BYTE;
            }
            else if (object instanceof Double) {
                return DOUBLE;
            }
            else if (object instanceof Float) {
                return FLOAT;
            }
            else if (object instanceof Integer) {
                return INTEGER;
            }
            else if (object instanceof Long) {
                return LONG;
            }
            else if (object instanceof Short) {
                return SHORT;
            }
            return NUMBER;
        }
        else if (object instanceof JSONObject) {
            return JSON_OBJECT;
        }
        else if (object instanceof JSONArray) {
            return JSON_ARRAY;
        }
        else if (object instanceof JsonNode) {
            JsonNode node = (JsonNode) object;
            JsonNodeType nodeType = node.getNodeType();
            switch (nodeType) {
                case ARRAY:
                    return JACKSON_ARRAY;
                case BINARY:
                    return JACKSON_BINARY;
                case BOOLEAN:
                    return JACKSON_BOOLEAN;
                case MISSING:
                    return JACKSON_MISSING;
                case NULL:
                    return JACKSON_NULL;
                case NUMBER:
                    return JACKSON_NUMBER;
                case OBJECT:
                    return JACKSON_OBJECT;
                case POJO:
                    return JACKSON_POJO;
                case STRING:
                    return JACKSON_STRING;
            }
            throw new IllegalStateException("unknown node type");
        }
        else if (object instanceof Map) {
            return MAP;
        }
        else if (object instanceof Boolean) {
            return BOOLEAN;
        }
        else if (object instanceof List) {
            return LIST;
        }
        else if (object instanceof byte[]) {
            return BYTE_ARRAY;
        }
        else {
            return UNKNOWN;
        }
    }
}
