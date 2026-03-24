package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.payment.impl.ui.WalletSelectorBottomSheet
import com.tangem.features.tangempay.model.TangemPayWalletSelectorModel
import com.tangem.features.tangempay.onboarding.impl.R

internal class TangemPayWalletSelectorComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayWalletSelectorModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        WalletSelectorBottomSheet(
            state = state,
            title = stringResourceSafe(R.string.common_choose_wallet),
        )
    }

    data class Params(
        val listener: WalletSelectorListener,
        val walletsIds: List<UserWalletId>,
    )
}

internal interface WalletSelectorListener {
    fun onWalletSelected(userWalletId: UserWalletId)
    fun onWalletSelectorDismiss()
}