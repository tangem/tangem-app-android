package com.tangem.core.remote.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.utils.logging.TangemLogger
import java.lang.reflect.Type

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class SkipMalformedElements

object SkipMalformedElementsAdapterFactory : JsonAdapter.Factory {

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        Types.nextAnnotations(annotations, SkipMalformedElements::class.java) ?: return null
        val elementType = Types.collectionElementType(type, List::class.java)

        return SkipMalformedElementsAdapter(elementAdapter = moshi.adapter<Any>(elementType)).nullSafe()
    }
}

private class SkipMalformedElementsAdapter(
    private val elementAdapter: JsonAdapter<Any>,
) : JsonAdapter<List<Any>>() {

    override fun fromJson(reader: JsonReader): List<Any> = buildList {
        reader.beginArray()
        while (reader.hasNext()) {
            // Decode a peeked copy so a malformed element leaves the reader at its start, then step over it
            try {
                elementAdapter.fromJson(reader.peekJson())?.let(::add)
            } catch (e: JsonDataException) {
                TangemLogger.e("Dropped a malformed element at ${reader.path}", e)
            }
            reader.skipValue()
        }
        reader.endArray()
    }

    override fun toJson(writer: JsonWriter, value: List<Any>?) {
        writer.beginArray()
        value?.forEach { elementAdapter.toJson(writer, it) }
        writer.endArray()
    }
}