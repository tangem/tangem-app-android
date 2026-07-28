package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.model.TangemPayViewPinModel
import com.tangem.features.tangempay.ui.TangemPayViewPinContent

internal class TangemPayViewPinComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayViewPinModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = ::dismiss)
        DisableScreenshotsDisposableEffect()
        TangemPayViewPinContent(state = state)
    }

    data class Params(
        val listener: ViewPinListener,
        val walletId: UserWalletId,
        val cardId: String,
    )
}

internal interface ViewPinListener {
    fun onClickChangePin()
    fun onDismissViewPin()
}