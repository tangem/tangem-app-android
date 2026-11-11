package com.tangem.features.earn.impl.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.impl.sheet.model.EarnYieldSheetModel
import com.tangem.features.earn.impl.sheet.ui.EarnYieldSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class EarnYieldSheetComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    private val model: EarnYieldSheetModel = getOrCreateModel(params)

    override fun dismiss() {
        model.onDismissClick()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        EarnYieldSheetContent(state = state)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val callback: ModelCallback,
    )

    interface ModelCallback {

        fun onDismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnYieldSheetComponent
    }
}