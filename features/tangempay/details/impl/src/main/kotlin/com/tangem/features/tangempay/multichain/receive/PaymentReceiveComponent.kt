package com.tangem.features.tangempay.multichain.receive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Pay-specific multi-token "Receive assets" bottom sheet for an already-issued (Available) network.
 * Unlike the shared, single-currency `TokenReceiveComponent`, this sheet can show several tokens
 * (e.g. USDC + USDT) sharing one deposit address on the same network.
 */
internal class PaymentReceiveComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: PaymentReceiveModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        PaymentReceiveContent(state = state)
    }

    data class Params(
        val walletId: UserWalletId,
        val networkRawId: String,
        val onDismiss: () -> Unit,
    )
}