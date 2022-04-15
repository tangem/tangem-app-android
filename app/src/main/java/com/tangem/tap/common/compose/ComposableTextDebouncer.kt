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
    debounce: Long = 400,
    onValueChanged: (T) -> Unit
): ValueDebouncer<T> {
    return remember {
        ValueDebouncer(
            debounce = debounce,
            onValueChanged = { changedValue ->
                changedValue?.let { onValueChanged(it) }
            },
        )
    }
}