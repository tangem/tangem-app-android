package com.tangem.features.walletconnect.transaction.components.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.ui.send.WcSendTransactionModalBottomSheet

internal class WcSendTransactionComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcSendTransactionModel,
    private val feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private var feeSelectorBlockComponent: FeeSelectorBlockComponent? = null

    init {
        lifecycle.doOnResume {
            val state = model.uiState.value
            if (state?.transaction?.feeState is WcTransactionFeeState.Success) {
                feeSelectorBlockComponent?.updateState(state.feeSelectorUM)
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
            val feeSelectorUM = content?.feeSelectorUM
            val feeSelectorBlock = if (state.feeState !is WcTransactionFeeState.None && feeSelectorUM != null) {
                getFeeSelectorBlockComponent(feeSelectorUM)
            } else {
                null
            }
            WcSendTransactionModalBottomSheet(
                state = state,
                feeSelectorBlockComponent = feeSelectorBlock,
                feeSelectorUM = content?.feeSelectorUM ?: FeeSelectorUM.Loading,
                onClickTransactionRequest = model::showTransactionRequest,
                onBack = router::pop,
                onDismiss = ::dismiss,
                onClickAllowToSpend = model::onClickAllowToSpend,
            )
        }
    }

    private fun getFeeSelectorBlockComponent(feeSelectorUM: FeeSelectorUM): FeeSelectorBlockComponent {
        val local = feeSelectorBlockComponent
        return if (local != null) {
            local
        } else {
            val component = feeSelectorBlockComponentFactory.create(
                context = appComponentContext,
                params = FeeSelectorParams.FeeSelectorBlockParams(
                    state = feeSelectorUM,
                    onLoadFee = model::loadFee,
                    cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    feeStateConfiguration = model.feeStateConfiguration,
                    feeDisplaySource = FeeSelectorParams.FeeDisplaySource.BottomSheet,
                    analyticsCategoryName = WcAnalyticEvents.WC_CATEGORY_NAME,
                ),
                onResult = model::updateFee,
            )
            feeSelectorBlockComponent = component
            component
        }
    }
}