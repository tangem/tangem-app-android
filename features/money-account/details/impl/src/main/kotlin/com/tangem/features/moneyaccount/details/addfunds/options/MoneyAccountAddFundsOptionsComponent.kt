package com.tangem.features.moneyaccount.details.addfunds.options

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal class MoneyAccountAddFundsOptionsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountAddFundsOptionsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        MoneyAccountAddFundsOptionsBottomSheet(state = state, onDismiss = model::onDismiss)
    }

    data class Params(
        val depositAddress: String?,
        val listener: MoneyAccountAddFundsOptionsListener,
    )
}

internal interface MoneyAccountAddFundsOptionsListener {
    fun onReceiveClick(depositAddress: String)
    fun onDismissAddFunds()
}