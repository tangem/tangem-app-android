package com.tangem.datasource.utils

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Kotlinx Serialization serializer for [androidx.datastore.core.DataStore]
 *
 */
class KotlinxDataStoreSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
    private val json: Json = DefaultJson,
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T {
        return try {
            input.bufferedReader().use { reader ->
                json.decodeFromString(
                    deserializer = serializer,
                    string = reader.readText(),
                )
            }
        } catch (e: Exception) {
            throw CorruptionException("Failed to deserialize data", e)
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.write(
                json.encodeToString(
                    serializer = serializer,
                    value = t,
                ),
            )
        }
    }

    private companion object {

        val DefaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}