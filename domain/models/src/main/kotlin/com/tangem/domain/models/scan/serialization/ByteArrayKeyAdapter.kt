package com.tangem.domain.models.scan.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.tangem.common.extensions.ByteArrayKey

class ByteArrayKeyAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, src: ByteArrayKey, byteArrayAdapter: JsonAdapter<ByteArray>) {
        byteArrayAdapter.toJson(writer, src.bytes)
    }

    @FromJson
    fun fromJson(reader: JsonReader, byteArrayAdapter: JsonAdapter<ByteArray>): ByteArrayKey? {
        return byteArrayAdapter.fromJson(reader)?.let {
            ByteArrayKey(bytes = it)
        }
    }
}