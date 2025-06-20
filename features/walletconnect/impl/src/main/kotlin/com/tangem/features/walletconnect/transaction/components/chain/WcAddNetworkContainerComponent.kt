package com.tangem.features.walletconnect.transaction.components.chain

import com.arkivanov.decompose.ComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.walletconnect.transaction.components.common.WcCommonTransactionComponentDelegate
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.components.common.getWcCommonScreen
import com.tangem.features.walletconnect.transaction.model.WcAddNetworkModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal class WcAddNetworkContainerComponent(
    appComponentContext: AppComponentContext,
    params: WcTransactionModelParams,
    feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : WcCommonTransactionComponentDelegate(appComponentContext, feeSelectorBlockComponentFactory) {

    private val model: WcAddNetworkModel = getOrCreateModel(params = params)

    init {
        super.init(navigation = model.stackNavigation, screenChild = ::screenChild, screenKey = "WcAddNetworkStack")
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

    private fun createTransactionComponent(appComponentContext: AppComponentContext): WcAddNetworkComponent {
        return WcAddNetworkComponent(appComponentContext = appComponentContext, model = model)
    }
}