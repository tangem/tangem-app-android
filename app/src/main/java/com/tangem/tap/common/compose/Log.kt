package com.tangem.tap.common.compose

import androidx.compose.runtime.Composable
import timber.log.Timber

/**
 * Simple logger for all recompositions
 */
@Composable
fun LogSideEffect(message: String) {
    Timber.d("SideEffect: $message")
}