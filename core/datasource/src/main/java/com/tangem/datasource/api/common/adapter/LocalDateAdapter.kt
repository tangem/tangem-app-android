package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

internal class LocalDateAdapter : JsonAdapter<LocalDate>() {

    private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    @FromJson
    override fun fromJson(reader: JsonReader): LocalDate? {
        return if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<LocalDate>()
        } else {
            val dateString = reader.nextString()
            return LocalDate.parse(dateString, formatter)
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: LocalDate?) {
        if (value != null) {
            writer.value(formatter.print(value))
        } else {
            writer.nullValue()
        }
    }
}