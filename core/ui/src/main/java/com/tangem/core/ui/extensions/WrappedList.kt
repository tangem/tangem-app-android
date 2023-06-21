package com.tangem.core.ui.extensions

import androidx.compose.runtime.Immutable

/**
[REDACTED_AUTHOR]
 */
@JvmInline
@Immutable
value class WrappedList<T>(val data: List<T>) : List<T> by data