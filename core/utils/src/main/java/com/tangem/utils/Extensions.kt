package com.tangem.utils

import java.math.BigDecimal

inline fun <reified T : Enum<T>> safeValueOf(type: String, default: T): T {
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        default
    }
}

fun BigDecimal?.isNullOrZero(): Boolean {
    return this == null || this.compareTo(BigDecimal.ZERO) == 0
}