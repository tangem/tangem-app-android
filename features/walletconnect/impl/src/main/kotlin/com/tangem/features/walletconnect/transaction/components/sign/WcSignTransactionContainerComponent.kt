package com.tangem.features.walletconnect.transaction.components.sign

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionRequestInfoComponent
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal class WcSignTransactionContainerComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val contentNavigation = StackNavigation<WcTransactionRoutes>()
    private val innerRouter = InnerRouter<WcTransactionRoutes>(
        stackNavigation = contentNavigation,
        popCallback = { onChildBack() },
    )
    private val model: WcSignTransactionModel = getOrCreateModel(params = params, router = innerRouter)
    private val contentStack = childStack(
        key = "WcSignTransactionStack",
        source = contentNavigation,
        serializer = WcTransactionRoutes.serializer(),
        initialConfiguration = WcTransactionRoutes.Transaction,
        handleBackButton = true,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val content by contentStack.subscribeAsState()
        BackHandler(onBack = ::onChildBack)
        content.active.instance.BottomSheet()
    }

    private fun onChildBack() {
        when (contentStack.value.active.configuration) {
            WcTransactionRoutes.Transaction,
            WcTransactionRoutes.TransactionRequestInfo,
            -> contentNavigation.pop()
        }
    }

    private fun screenChild(
        config: WcTransactionRoutes,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val appComponentContext = childByContext(componentContext = componentContext, router = innerRouter)
        return when (config) {
            is WcTransactionRoutes.Transaction -> WcSignTransactionComponent(
                appComponentContext = appComponentContext,
                onDismiss = model::dismiss,
                model = model,
                transactionInfoOnClick = { contentNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo) },
            )
            is WcTransactionRoutes.TransactionRequestInfo -> WcTransactionRequestInfoComponent(
                appComponentContext = appComponentContext,
                onDismiss = model::dismiss,
                model = model.uiState.value?.transactionRequestInfo,
            )
        }
    }

    data class Params(val rawRequest: WcSdkSessionRequest)
}