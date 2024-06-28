package com.tangem.features.details.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext

interface UserWalletListComponent {

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    fun View(modifier: Modifier)

    interface Factory {
        fun create(context: AppComponentContext): UserWalletListComponent
    }
}