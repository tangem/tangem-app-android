package com.tangem.features.walletconnect.transaction.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.features.walletconnect.transaction.model.WcAddNetworkModel
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionCommonBottomSheetTitle

internal class WcAddNetworkContainerComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcAddNetworkModel = getOrCreateModel(params = params)
    private val contentNavigation = StackNavigation<WcTransactionRoutes>()
    private val contentStack = childStack(
        source = contentNavigation,
        serializer = WcTransactionRoutes.serializer(),
        initialConfiguration = WcTransactionRoutes.Transaction,
        childFactory = ::screenChild,
    )

    private fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val content by contentStack.subscribeAsState()
        TangemModalBottomSheet<WcTransactionRoutes>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = content.active.configuration,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = { config ->
                WcTransactionCommonBottomSheetTitle(
                    route = config,
                    onStartClick = ::toTransaction,
                    onEndClick = ::dismiss,
                )
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    Children(stack = content) { child ->
                        child.instance.Content(modifier = Modifier)
                    }
                }
            },
        )
    }

    private fun toTransactionRequestInfo() {
        contentNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun toTransaction() {
        contentNavigation.pushNew(WcTransactionRoutes.Transaction)
    }

    private fun screenChild(
        config: WcTransactionRoutes,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        is WcTransactionRoutes.Transaction -> WcAddNetworkComponent(
            appComponentContext = childByContext(componentContext),
            model = model,
            transactionInfoOnClick = ::toTransactionRequestInfo,
        )
        is WcTransactionRoutes.TransactionRequestInfo -> WcTransactionRequestInfoComponent(
            appComponentContext = childByContext(componentContext),
            model = model.uiState.value?.transactionRequestInfo,
        )
    }

    data class Params(val rawRequest: WcSdkSessionRequest)
}