package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.model.TangemPayReissueCardModel
import com.tangem.features.tangempay.ui.TangemPayReissueCardContent

internal class TangemPayReissueCardComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayReissueCardModel = getOrCreateModel(params = params)

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        TangemPayReissueCardContent(state = state)
    }

    data class Params(
        val listener: ReissueCardListener,
        val userWalletId: UserWalletId,
        val cardId: String,
    )
}

internal interface ReissueCardListener {
    fun onDismissReissueCard()
    fun onClickAddFunds()
}