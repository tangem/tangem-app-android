package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.model.TangemPayIssueAdditionalCardModel
import com.tangem.features.tangempay.ui.TangemPayIssueAdditionalCardContent

internal class TangemPayIssueAdditionalCardComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayIssueAdditionalCardModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayIssueAdditionalCardContent(state = state)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val feeAmount: SerializedBigDecimal,
        val feeCurrency: SerializedCurrency,
        val fiatBalance: SerializedBigDecimal,
        val listener: Listener,
    )

    interface Listener {
        fun onIssueAdditionalCardDismissed()
        fun onIssueAdditionalCardSucceeded()
        fun onAddFundsForCardIssue()
    }
}