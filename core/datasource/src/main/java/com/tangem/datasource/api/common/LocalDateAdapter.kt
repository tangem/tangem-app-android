package com.tangem.datasource.api.common

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

class LocalDateAdapter : JsonAdapter<LocalDate>() {

    private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    @FromJson
    override fun fromJson(reader: JsonReader): LocalDate? {
        val dateString = reader.nextString()
        return LocalDate.parse(dateString, formatter)
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