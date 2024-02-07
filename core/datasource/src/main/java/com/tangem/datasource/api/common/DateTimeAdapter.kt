package com.tangem.datasource.api.common

import com.squareup.moshi.*
import org.joda.time.DateTime

internal class DateTimeAdapter : JsonAdapter<DateTime>() {

    @FromJson
    override fun fromJson(reader: JsonReader): DateTime? {
        val dateString = reader.nextString() ?: return null

        return DateTime.parse(dateString)
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