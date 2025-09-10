package com.tangem.features.walletconnect.transaction.components.common

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams.FeeDisplaySource
import com.tangem.features.send.v2.api.params.FeeSelectorParams.FeeSelectorDetailsParams
import com.tangem.features.walletconnect.connections.components.AlertsComponentV2
import com.tangem.features.walletconnect.connections.utils.WcAlertsFactory.createCommonTransactionAppInfoAlertUM
import com.tangem.features.walletconnect.transaction.components.send.WcCustomAllowanceComponent
import com.tangem.features.walletconnect.transaction.components.send.WcSendingProcessComponent
import com.tangem.features.walletconnect.transaction.components.send.WcSendMultipleTransactionsComponent
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
                onDismiss = model::popBack,
                params = FeeSelectorDetailsParams(
                    state = state.feeSelectorUM,
                    onLoadFee = model::loadFee,
                    feeCryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    callback = model,
                    feeStateConfiguration = model.feeStateConfiguration,
                    feeDisplaySource = FeeDisplaySource.BottomSheet,
                    analyticsCategoryName = WcAnalyticEvents.WC_CATEGORY_NAME,
                ),
            )
        }
        is WcTransactionRoutes.MultipleTransactions -> {
            val model = model as? WcSendTransactionModel ?: error("model must be WcSendTransactionModel")
            WcSendMultipleTransactionsComponent(
                appComponentContext = appComponentContext,
                model = model,
                onConfirm = config.onConfirm,
            )
        }
        WcTransactionRoutes.TransactionProcess -> {
            val model = model as? WcSendTransactionModel ?: error("model must be WcSendTransactionModel")
            WcSendingProcessComponent(
                appComponentContext = appComponentContext,
                model = model,
            )
        }
    }
}