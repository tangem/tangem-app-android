package com.tangem.features.walletconnect.transaction.components.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.ui.send.WcSendTransactionModalBottomSheet

internal class WcSendTransactionComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcSendTransactionModel,
    private val feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val feeSelectorBlockComponent by lazy {
        feeSelectorBlockComponentFactory.create(
            context = appComponentContext,
            params = FeeSelectorParams.FeeSelectorBlockParams(
                onLoadFee = model::loadFee,
                network = model.useCase.network,
                cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                suggestedFeeState = model.suggestedFeeState,
            ),
        )
    }

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by model.uiState.collectAsStateWithLifecycle()

        if (content != null) {
            WcSendTransactionModalBottomSheet(
                state = content!!.transaction,
                feeSelectorBlockComponent = feeSelectorBlockComponent,
                onClickTransactionRequest = model::showTransactionRequest,
                onBack = router::pop,
                onDismiss = ::dismiss,
                onClickAllowToSpend = model::onClickAllowToSpend,
            )
        }
    }
}