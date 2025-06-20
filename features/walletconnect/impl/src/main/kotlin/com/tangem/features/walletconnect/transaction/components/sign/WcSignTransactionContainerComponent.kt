package com.tangem.features.walletconnect.transaction.components.sign

import com.arkivanov.decompose.ComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.walletconnect.transaction.components.common.WcCommonTransactionComponentDelegate
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.components.common.getWcCommonScreen
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal class WcSignTransactionContainerComponent(
    appComponentContext: AppComponentContext,
    params: WcTransactionModelParams,
    feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : WcCommonTransactionComponentDelegate(appComponentContext, feeSelectorBlockComponentFactory) {

    private val model: WcSignTransactionModel = getOrCreateModel(params = params)

    init {
        super.init(
            navigation = model.stackNavigation,
            screenChild = ::screenChild,
            screenKey = "WcSignTransactionStack",
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

    private fun createTransactionComponent(appComponentContext: AppComponentContext): WcSignTransactionComponent {
        return WcSignTransactionComponent(
            appComponentContext = appComponentContext,
            model = model,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
        )
    }
}