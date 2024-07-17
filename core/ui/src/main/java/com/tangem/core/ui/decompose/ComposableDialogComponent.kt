package com.tangem.core.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface ComposableDialogComponent {

    fun dismiss()

    @Composable
    fun Dialog()
}