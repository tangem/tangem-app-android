package com.tangem.datasource.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * Factory to serialize nulls in Moshi if the class is annotated with [SerializeNulls].
 *
[REDACTED_AUTHOR]
 */
internal object SerializeNullsFactory : JsonAdapter.Factory {

    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val rawType = Types.getRawType(type)
        if (!rawType.isAnnotationPresent(SerializeNulls::class.java)) {
            return null
        }

        val nextAdapter: JsonAdapter<Any> = moshi.nextAdapter(this, type, annotations)

        return nextAdapter.serializeNulls()
    }
}