package com.tangem.features.onboarding.usedcard.syncwallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

internal class SyncWalletComponent(
    context: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    data class Params(
        val onContinueClick: () -> Unit,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        SyncWalletScreen(
            onContinueClick = params.onContinueClick,
            modifier = modifier,
        )
    }
}