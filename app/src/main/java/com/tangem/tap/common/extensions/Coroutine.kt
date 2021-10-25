package com.tangem.tap.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
[REDACTED_AUTHOR]
 */
suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T) {
    withContext(Dispatchers.Main, block)
}

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T) {
    withContext(Dispatchers.IO, block)
}