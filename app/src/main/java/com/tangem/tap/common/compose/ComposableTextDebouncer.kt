package com.tangem.tap.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tangem.domain.common.ValueDebouncer

/**
[REDACTED_AUTHOR]
 * This is an empty compose view. It just remember the ValueDebouncer inside of itself.
 */
@Composable
fun ComposableTextDebouncer(
    text: String,
    debounce: Long = 400,
    onTextChanged: (String) -> Unit
): ValueDebouncer<String> {
    val rTextDebounce = remember {
        mutableStateOf(ValueDebouncer(text, debounce) { changedValue ->
            changedValue?.let { onTextChanged(it) }
        })
    }
    rTextDebounce.value.value = text

    return rTextDebounce.value
}