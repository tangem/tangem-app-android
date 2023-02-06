package com.tangem.utils

inline fun <reified T : Enum<T>> safeValueOf(type: String, default: T): T {
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        default
    }
}
