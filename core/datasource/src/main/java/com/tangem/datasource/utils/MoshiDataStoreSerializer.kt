package com.tangem.datasource.utils

import androidx.datastore.core.Serializer
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.ParameterizedType

/**
 * Moshi serializer [JsonAdapter] for [androidx.datastore.core.DataStore]
 *
 * @property defaultValue default value
 * @property adapter      moshi adapter
 *
[REDACTED_AUTHOR]
 */
class MoshiDataStoreSerializer<T>(
    override val defaultValue: T,
    private val adapter: JsonAdapter<T>,
) : Serializer<T> {

    /**
     * Constructor
     *
     * @param moshi        moshi for creating adapter
     * @param types        types of data
     * @param defaultValue default value
     */
    constructor(moshi: Moshi, types: ParameterizedType, defaultValue: T) : this(
        defaultValue = defaultValue,
        adapter = moshi.adapter<T>(types),
    )

    override suspend fun readFrom(input: InputStream): T {
        return input.bufferedReader().use { reader ->
            adapter.fromJson(reader.readText()) ?: defaultValue
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.bufferedWriter().use { write ->
            write.write(adapter.toJson(t))
        }
    }
}