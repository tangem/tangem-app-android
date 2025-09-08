package com.tangem.features.yieldlending.impl.subcomponents.active

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.yieldlending.impl.subcomponents.active.model.YieldLendingActiveModel
import com.tangem.features.yieldlending.impl.subcomponents.active.ui.YieldLendingActiveBottomSheet
import kotlinx.coroutines.flow.StateFlow

internal class YieldLendingActiveComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: YieldLendingActiveModel = getOrCreateModel(params = params)

    override fun dismiss() {
        params.onDismiss()
    }
    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        YieldLendingActiveBottomSheet(config = state.bottomSheetConfig, onCloseClick = {}, onClick = {})
    }

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val onDismiss: () -> Unit,
    )
}