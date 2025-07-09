package com.tangem.features.walletconnect.transaction.components.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.ui.send.WcSendTransactionModalBottomSheet

internal class WcSendTransactionComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcSendTransactionModel,
    private val feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val feeSelectorBlockComponent by lazy {
        val state = requireNotNull(model.uiState.value) { "in this step state should be not null" }
        feeSelectorBlockComponentFactory.create(
            context = appComponentContext,
            params = FeeSelectorParams.FeeSelectorBlockParams(
                state = state.feeSelectorUM,
                onLoadFee = model::loadFee,
                cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                suggestedFeeState = model.suggestedFeeState,
                feeDisplaySource = FeeSelectorParams.FeeDisplaySource.BottomSheet,
            ),
            onResult = model::updateFee,
        )
    }

    init {
        lifecycle.doOnResume {
            val state = model.uiState.value
            if (state?.transaction?.feeState is WcTransactionFeeState.Success) {
                feeSelectorBlockComponent.updateState(state.feeSelectorUM)
            }
        }
    }

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by model.uiState.collectAsStateWithLifecycle()
        val state = content?.transaction

        if (state != null) {
            val feeSelectorBlock =
                if (state.feeState !is WcTransactionFeeState.None) feeSelectorBlockComponent else null
            WcSendTransactionModalBottomSheet(
                state = state,
                feeSelectorBlockComponent = feeSelectorBlock,
                onClickTransactionRequest = model::showTransactionRequest,
                onBack = router::pop,
                onDismiss = ::dismiss,
                onClickAllowToSpend = model::onClickAllowToSpend,
            )
        }
    }
}