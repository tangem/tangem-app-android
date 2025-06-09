package com.tangem.utils.extensions

fun <K, V, R> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    return this
        .mapNotNull { entry ->
            val newValue = transform(entry) ?: return@mapNotNull null

            entry.key to newValue
        }
        .toMap()
}