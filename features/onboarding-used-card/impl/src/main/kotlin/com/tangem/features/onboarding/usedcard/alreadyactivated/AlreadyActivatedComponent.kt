package com.tangem.features.onboarding.usedcard.alreadyactivated

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

internal class AlreadyActivatedComponent(
    context: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    data class Params(
        val onThisIsMyWalletClick: () -> Unit,
        val onNewCardClick: () -> Unit,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        AlreadyActivatedScreen(
            onThisIsMyWalletClick = params.onThisIsMyWalletClick,
            onNewCardClick = params.onNewCardClick,
            modifier = modifier,
        )
    }
}