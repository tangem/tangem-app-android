package com.tangem.features.walletconnect.transaction.components.common

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams.FeeDisplaySource
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams.FeeSelectorDetailsParams
import com.tangem.features.walletconnect.connections.components.AlertsComponentV2
import com.tangem.features.walletconnect.connections.utils.WcAlertsFactory.createCommonTransactionAppInfoAlertUM
import com.tangem.features.walletconnect.transaction.components.send.WcCustomAllowanceComponent
import com.tangem.features.walletconnect.transaction.components.send.WcSendMultipleTransactionsComponent
import com.tangem.features.walletconnect.transaction.components.send.WcSendingProcessComponent
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
            messageUM = createCommonTransactionAppInfoAlertUM(config.alertType),
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
            val sendModel = model as? WcSendTransactionModel ?: error("model must be WcSendTransactionModel")
            val state = sendModel.uiState.value ?: error("in this step state should be not null")
            feeSelectorComponentFactory.create(
                context = appComponentContext,
                onDismiss = sendModel::popBack,
                params = FeeSelectorDetailsParams(
                    state = state.feeSelectorUM,
                    onLoadFee = sendModel::loadFee,
                    feeCryptoCurrencyStatus = sendModel.cryptoCurrencyStatus,
                    cryptoCurrencyStatus = sendModel.cryptoCurrencyStatus,
                    callback = sendModel,
                    feeStateConfiguration = sendModel.feeStateConfiguration,
                    feeDisplaySource = FeeDisplaySource.BottomSheet,
                    analyticsCategoryName = WcAnalyticEvents.WC_CATEGORY_NAME,
                    analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.WalletConnect,
                    userWalletId = sendModel.userWallet.walletId,
                ),
            )
        }
        is WcTransactionRoutes.MultipleTransactions -> {
            val sendModel = model as? WcSendTransactionModel ?: error("model must be WcSendTransactionModel")
            WcSendMultipleTransactionsComponent(
                appComponentContext = appComponentContext,
                model = sendModel,
                onConfirm = { sendModel.onMultiTransactionConfirm() },
            )
        }
        WcTransactionRoutes.TransactionProcess -> {
            val sendModel = model as? WcSendTransactionModel ?: error("model must be WcSendTransactionModel")
            WcSendingProcessComponent(
                appComponentContext = appComponentContext,
                model = sendModel,
            )
        }
    }
}