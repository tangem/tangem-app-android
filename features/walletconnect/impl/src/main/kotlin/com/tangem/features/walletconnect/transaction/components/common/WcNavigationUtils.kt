package com.tangem.features.walletconnect.transaction.components.common

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.walletconnect.connections.components.AlertsComponentV2
import com.tangem.features.walletconnect.connections.utils.WcAlertsFactory.createCommonTransactionAppInfoAlertUM
import com.tangem.features.walletconnect.transaction.components.send.WcCustomAllowanceComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal fun getWcCommonScreen(
    config: WcTransactionRoutes,
    appComponentContext: AppComponentContext,
    transactionScreenConverter: () -> ComposableBottomSheetComponent,
    model: WcCommonTransactionModel,
    feeSelectorComponentFactory: FeeSelectorComponent.Factory? = null,
): ComposableBottomSheetComponent {
    return when (config) {
        is WcTransactionRoutes.Transaction -> transactionScreenConverter()
        is WcTransactionRoutes.Alert -> AlertsComponentV2(
            appComponentContext = appComponentContext,
            messageUM = createCommonTransactionAppInfoAlertUM(config.type),
        )
        is WcTransactionRoutes.TransactionRequestInfo -> WcTransactionRequestInfoComponent(
            appComponentContext = appComponentContext,
            model = model,
        )
        is WcTransactionRoutes.CustomAllowance -> WcCustomAllowanceComponent(
            appComponentContext = appComponentContext,
            model = model,
            onClickDoneCustomAllowance = { value, isUnlimited ->
                (model as? WcSendTransactionModel)?.onClickDoneCustomAllowance(value, isUnlimited)
            },
        )
        is WcTransactionRoutes.SelectFee -> {
            requireNotNull(feeSelectorComponentFactory) { "feeSelectorComponentFactory must not be null" }
            val model = model as? WcSendTransactionModel ?: error("model must be WcSendTransactionModel")
            val state = model.uiState.value ?: error("in this step state should be not null")
            feeSelectorComponentFactory.create(
                context = appComponentContext,
                params = FeeSelectorParams.FeeSelectorDetailsParams(
                    state = state.feeSelectorUM,
                    onLoadFee = model::loadFee,
                    cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    callback = model,
                    suggestedFeeState = model.suggestedFeeState,
                ),
            )
        }
    }
}