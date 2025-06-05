package com.tangem.features.walletconnect.transaction.components.send

import com.arkivanov.decompose.ComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.components.common.WcCommonTransactionComponentDelegate
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.components.common.getWcCommonScreen
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal class WcSendTransactionContainerComponent(
    appComponentContext: AppComponentContext,
    params: WcTransactionModelParams,
) : WcCommonTransactionComponentDelegate(appComponentContext) {

    private val model: WcSendTransactionModel = getOrCreateModel(params = params)

    init {
        super.init(
            navigation = model.stackNavigation,
            screenChild = ::screenChild,
            screenKey = "WcSendTransactionStack",
        )
    }

    private fun screenChild(
        config: WcTransactionRoutes,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val appComponentContext = childByContext(componentContext = componentContext, router = innerRouter)
        return getWcCommonScreen(
            config = config,
            appComponentContext = appComponentContext,
            transactionScreenConverter = { createTransactionComponent(appComponentContext) },
            model = model,
        )
    }

    private fun createTransactionComponent(appComponentContext: AppComponentContext): WcSendTransactionComponent {
        return WcSendTransactionComponent(appComponentContext = appComponentContext, model = model)
    }
}