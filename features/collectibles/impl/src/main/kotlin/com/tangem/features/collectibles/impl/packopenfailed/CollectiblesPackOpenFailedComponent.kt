package com.tangem.features.collectibles.impl.packopenfailed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.backhandler.BackCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.packopenfailed.ui.CollectiblesPackOpenFailedScreen

/** A terminal failure of the pack opening: it replaces the confirm order, so both close and back leave for main. */
internal class CollectiblesPackOpenFailedComponent(
    context: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    init {
        // Back would otherwise land on the machine details the confirm order was opened from
        backHandler.register(BackCallback(onBack = ::onCloseClick))
    }

    @Composable
    override fun Content(modifier: Modifier) {
        CollectiblesPackOpenFailedScreen(
            onGetAssistanceClick = ::onGetAssistanceClick,
            onCloseClick = ::onCloseClick,
            modifier = modifier,
        )
    }

    private fun onCloseClick() {
        router.popTo(CollectiblesRoute.Main(params.userWalletId))
    }

    @Suppress("EmptyFunctionBlock")
    private fun onGetAssistanceClick() {
        // TODO([REDACTED_TASK_KEY]): support entry point is not wired yet.
    }

    data class Params(val userWalletId: UserWalletId)
}