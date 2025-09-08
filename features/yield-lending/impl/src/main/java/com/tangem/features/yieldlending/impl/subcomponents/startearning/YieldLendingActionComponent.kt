package com.tangem.features.yieldlending.impl.subcomponents.startearning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.yieldlending.impl.subcomponents.startearning.model.YieldLendingActionModel
import com.tangem.features.yieldlending.impl.subcomponents.startearning.ui.YieldLendingStartEarningBottomSheet

internal class YieldLendingActionComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: YieldLendingActionModel = getOrCreateModel(params = params)
    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        YieldLendingStartEarningBottomSheet(
            config = state.bottomSheetConfig,
            onClick = model::onClick,
        )
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val onDismiss: () -> Unit,
    )
}