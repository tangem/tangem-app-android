package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.*
import com.tangem.core.analytics.models.EventValue

class EventValueJsonAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): EventValue {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> {
                EventValue.StringValue(reader.nextString())
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<String>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(reader.nextString())
                }
                reader.endArray()
                EventValue.ListValue(list)
            }
            else -> throw JsonDataException("Unexpected token: ${reader.peek()}")
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: EventValue?) {
        when (value) {
            is EventValue.StringValue -> writer.value(value.value)
            is EventValue.ListValue -> {
                writer.beginArray()
                for (item in value.value) {
                    writer.value(item)
                }
                writer.endArray()
            }
            null -> writer.nullValue()
        }
    }
}