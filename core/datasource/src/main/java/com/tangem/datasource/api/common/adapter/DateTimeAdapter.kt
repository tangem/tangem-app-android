package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.*
import org.joda.time.DateTime

internal class DateTimeAdapter : JsonAdapter<DateTime>() {

    @FromJson
    override fun fromJson(reader: JsonReader): DateTime? {
        return if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<DateTime>()
        } else {
            val dateString = reader.nextString()
            DateTime.parse(dateString)
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: DateTime?) {
        if (value != null) {
            writer.value(value.toString())
        } else {
            writer.nullValue()
        }
    }
}