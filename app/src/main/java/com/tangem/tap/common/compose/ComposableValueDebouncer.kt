package com.tangem.tap.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.domain.common.util.ValueDebouncer

/**
 * Created by Anton Zhilenkov on 06/04/2022.
 * This is an empty compose view. It just remember the ValueDebouncer inside of itself.
 */
@Composable
fun <T> valueDebouncerAsState(
    initialValue: T,
    onValueChange: (T) -> Unit,
    debounce: Long = 600,
    onEmitValueReceive: (T) -> Unit = {},
): ValueDebouncer<T> {
    return remember {
        ValueDebouncer(
            initialValue = initialValue,
            debounceDuration = debounce,
            onEmitValueReceived = { emitValue ->
                emitValue?.let { onEmitValueReceive(it) }
            },
            onValueChanged = { changedValue ->
                changedValue?.let { onValueChange(it) }
            },
        )
    }
}

@Composable
fun <T> valueDebouncerNullableAsState(
    initialValue: T?,
    onValueChange: (T?) -> Unit,
    debounce: Long = 400,
    onEmitValueReceive: (T?) -> Unit = {},
): ValueDebouncer<T?> {
    return remember {
        ValueDebouncer(
            initialValue = initialValue,
            debounceDuration = debounce,
            onEmitValueReceived = onEmitValueReceive,
            onValueChanged = onValueChange,
        )
    }
}
