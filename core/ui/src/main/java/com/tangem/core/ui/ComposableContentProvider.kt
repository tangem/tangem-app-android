package com.tangem.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
fun interface ComposableContentProvider {

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    fun Content(modifier: Modifier)
}