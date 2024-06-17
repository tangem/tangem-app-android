package com.tangem.common.routing.entity

import android.os.Bundle
import kotlinx.serialization.Serializable

@Serializable
data class SerializableBundle(
    val map: Map<String, String>,
) {

    constructor(bundle: Bundle) : this(
        map = bundle.keySet().mapNotNull { key ->
            bundle.getString(key)?.let { key to it }
        }.toMap(),
    )

    fun toBundle(): Bundle {
        return Bundle().apply {
            map.forEach { (key, value) ->
                putString(key, value)
            }
        }
    }
}