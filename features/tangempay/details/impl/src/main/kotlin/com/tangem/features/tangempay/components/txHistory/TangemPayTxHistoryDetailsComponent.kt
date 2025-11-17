package com.tangem.features.tangempay.components.txHistory

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.*
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.model.TangemPayTxHistoryDetailsModel
import com.tangem.features.tangempay.ui.TangemPayTxHistoryDetailsContent

internal class TangemPayTxHistoryDetailsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayTxHistoryDetailsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        TangemPayTxHistoryDetailsContent(state = model.uiState)
    }

    data class Params(
        val transaction: TangemPayTxHistoryItem,
        val userWalletId: UserWalletId,
        val onDismiss: () -> Unit,
    )
}