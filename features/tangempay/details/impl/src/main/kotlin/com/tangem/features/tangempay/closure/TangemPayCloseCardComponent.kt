package com.tangem.features.tangempay.closure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

internal class TangemPayCloseCardComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayCloseCardModel = getOrCreateModel(params = params)

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        TangemPayCloseCardContent(state = state)
    }

    data class Params(
        val listener: CloseCardListener,
        val userWalletId: UserWalletId,
        val cardId: String,
    )
}

internal interface CloseCardListener {
    fun onDismissCloseCard()
}