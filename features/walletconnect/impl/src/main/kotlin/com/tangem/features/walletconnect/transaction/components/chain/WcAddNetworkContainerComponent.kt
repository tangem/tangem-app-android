package com.tangem.features.walletconnect.transaction.components.chain

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionRequestInfoComponent
import com.tangem.features.walletconnect.transaction.model.WcAddNetworkModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal class WcAddNetworkContainerComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val contentNavigation = StackNavigation<WcTransactionRoutes>()
    private val innerRouter = InnerRouter<WcTransactionRoutes>(
        stackNavigation = contentNavigation,
        popCallback = { onChildBack() },
    )
    private val model: WcAddNetworkModel = getOrCreateModel(params = params, router = innerRouter)
    private val contentStack = childStack(
        key = "WcAddNetworkStack",
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

    private fun dismiss() {
        model.dismiss()
    }

    private fun onChildBack() {
        when (contentStack.value.active.configuration) {
            WcTransactionRoutes.Transaction -> contentNavigation.pop()
            WcTransactionRoutes.TransactionRequestInfo -> contentNavigation.pushNew(WcTransactionRoutes.Transaction)
        }
    }

    private fun toTransactionRequestInfo() {
        contentNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun screenChild(
        config: WcTransactionRoutes,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val appComponentContext = childByContext(componentContext = componentContext, router = innerRouter)
        return when (config) {
            is WcTransactionRoutes.Transaction -> WcAddNetworkComponent(
                appComponentContext = childByContext(componentContext),
                transactionInfoOnClick = ::toTransactionRequestInfo,
                onDismiss = ::dismiss,
                model = model,
            )
            is WcTransactionRoutes.TransactionRequestInfo -> WcTransactionRequestInfoComponent(
                appComponentContext = appComponentContext,
                onDismiss = ::dismiss,
                model = model.uiState.value?.transactionRequestInfo,
            )
        }
    }

    data class Params(val rawRequest: WcSdkSessionRequest)
}