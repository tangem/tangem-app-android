package com.tangem.core.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface ComposableDialogComponent {

    val doOnDismiss: () -> Unit

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    fun Dialog()
}