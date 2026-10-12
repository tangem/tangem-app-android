package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import okio.Buffer
import okio.BufferedSource

/**
 * Reads a JSON value as the text the server sent instead of decoding it into Kotlin types.
 *
 * Meant for a value the app hashes rather than reads: decoding one through Moshi's generic types and encoding it
 * back turns every number into a double (`137` into `137.0`), and a digest taken over the result silently stops
 * matching the one the server computed.
 */
@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class RawJson

internal class RawJsonAdapter {

    @FromJson
    @RawJson
    fun fromJson(reader: JsonReader): String = reader.nextSource().use(BufferedSource::readUtf8)

    @ToJson
    fun toJson(writer: JsonWriter, @RawJson value: String) {
        writer.value(Buffer().writeUtf8(value))
    }
}