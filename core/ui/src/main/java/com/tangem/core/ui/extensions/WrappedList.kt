package com.tangem.core.ui.extensions

import androidx.compose.runtime.Immutable

/**
 * @author Andrew Khokhlov on 06/06/2023
 */
@JvmInline
@Immutable
value class WrappedList<T>(val data: List<T>) : List<T> by data
