package com.tangem.feature.wallet.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.message.modal.MessageModal
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkRequester
import com.tangem.features.wallet.WalletLinkComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Serves [WalletLinkRequester] calls for its whole lifetime, so the root only creates and draws it. */
internal class DefaultWalletLinkComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    private val requesterProxy: WalletLinkRequesterProxy,
) : AppComponentContext by appComponentContext, WalletLinkComponent, WalletLinkRequester {

    private val model: WalletLinkModel = getOrCreateModel(params)

    init {
        requesterProxy.componentRequester.value = this
        lifecycle.doOnDestroy { requesterProxy.componentRequester.compareAndSet(expect = this, update = null) }
    }

    override suspend fun requestLink(userWallet: UserWallet): WalletLinkRequester.Result = model.request(userWallet)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        state?.let { messageModalUM ->
            MessageModal(
                state = messageModalUM,
                onDismissRequest = model::onDismiss,
                // A request can come from a screen showing a dialog-window sheet, which would cover an overlay
                presentation = TangemModal.Presentation.Dialog,
            )
        }
    }

    @AssistedFactory
    interface Factory : WalletLinkComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultWalletLinkComponent
    }
}