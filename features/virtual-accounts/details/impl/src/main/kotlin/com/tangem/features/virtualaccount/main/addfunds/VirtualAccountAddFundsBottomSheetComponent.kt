package com.tangem.features.virtualaccount.main.addfunds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId

internal class VirtualAccountAddFundsBottomSheetComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: VirtualAccountAddFundsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        VirtualAccountAddFundsBottomSheet(state = state)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val requisites: List<RequisitesRow>,
        val dailyDepositLimit: String,
        val listener: VirtualAccountAddFundsListener,
    )

    data class RequisitesRow(
        val title: TextReference,
        val titleForShare: String,
        val value: String,
    )
}

internal interface VirtualAccountAddFundsListener {
    fun onAddFundsDismiss()
}