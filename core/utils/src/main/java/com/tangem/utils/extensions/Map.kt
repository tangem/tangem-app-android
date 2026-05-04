package com.tangem.utils.extensions

inline fun <K, V, R> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val result = linkedMapOf<K, R>()
    this.forEach { entry ->
        val newValue = transform(entry) ?: return@forEach
        result[entry.key] = newValue
    }
    return result
}