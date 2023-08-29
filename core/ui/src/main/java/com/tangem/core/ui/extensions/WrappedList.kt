package com.tangem.core.ui.extensions

import androidx.compose.runtime.Immutable

/**
[REDACTED_AUTHOR]
 */
@JvmInline
@Immutable
value class WrappedList<T>(val data: List<T>) : List<T> by data

fun <T> List<T>.toWrappedList(): WrappedList<T> = WrappedList(data = this)

fun <T> wrappedList(vararg elements: T): WrappedList<T> = WrappedList(data = listOf(*elements))