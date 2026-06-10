package com.tangem.features.pushnotifications.impl.domain

enum class DoubleAskVariant(val key: String) {
    Off(key = "control"),
    On(key = "treatment"),
    ;

    companion object {
        fun fromKey(value: String): DoubleAskVariant =
            entries.firstOrNull { it.key.equals(value, ignoreCase = true) } ?: Off
    }
}