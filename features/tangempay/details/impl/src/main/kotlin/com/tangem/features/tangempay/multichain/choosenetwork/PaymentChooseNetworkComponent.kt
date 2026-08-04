package com.tangem.features.tangempay.multichain.choosenetwork

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

internal class PaymentChooseNetworkComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: PaymentChooseNetworkModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        PaymentChooseNetworkContent(state = state)
    }

    data class Params(
        val walletId: UserWalletId,
        val listener: ChooseNetworkListener,
    )
}

/**
 * Callbacks for the Choose-network bottom sheet, implemented by the parent (details/card page) models.
 *
 * Note: a not-yet-issued network tap is handled entirely inside [PaymentChooseNetworkModel] (it owns the
 * per-row Loading/Error state), so it is not part of this listener.
 */
internal interface ChooseNetworkListener {

    /**
     * User picked an already-issued network to receive on. Carries only the network's stable
     * [networkRawId] identity — the receive sheet re-resolves currencies and address from live status.
     */
    fun onSelectAvailable(networkRawId: String)

    /** User picked a network that is info-only ("Other ways" section). */
    fun onSelectDisabled()

    /** User dismissed the bottom sheet. */
    fun onDismiss()
}