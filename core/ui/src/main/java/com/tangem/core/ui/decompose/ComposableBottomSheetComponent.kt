package com.tangem.core.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface ComposableBottomSheetComponent {

    fun dismiss()

    @Composable
    fun BottomSheet()
}