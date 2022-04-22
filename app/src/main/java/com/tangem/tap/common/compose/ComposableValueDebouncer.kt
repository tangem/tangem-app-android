package com.tangem.tap.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.domain.common.util.ValueDebouncer

/**
[REDACTED_AUTHOR]
 * This is an empty compose view. It just remember the ValueDebouncer inside of itself.
 */
@Composable
fun <T> valueDebouncerAsState(
    initialValue: T,
    debounce: Long = 400,
    onEmitValueReceived: (T) -> Unit = {},
    onValueChanged: (T) -> Unit,
): ValueDebouncer<T> {
    return remember {
        ValueDebouncer<T>(
            initialValue = initialValue,
            debounceDuration = debounce,
            onEmitValueReceived = { emitValue ->
                emitValue?.let { onEmitValueReceived(it) }
            },
            onValueChanged = { changedValue ->
                changedValue?.let { onValueChanged(it) }
            },
        )
    }
}

@Composable
fun <T> valueDebouncerNullableAsState(
    initialValue: T?,
    debounce: Long = 400,
    onEmitValueReceived: (T?) -> Unit = {},
    onValueChanged: (T?) -> Unit,
): ValueDebouncer<T?> {
    return remember {
        ValueDebouncer(
            initialValue = initialValue,
            debounceDuration = debounce,
            onEmitValueReceived = onEmitValueReceived,
            onValueChanged = onValueChanged,
        )
    }
}