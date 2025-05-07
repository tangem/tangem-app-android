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
import com.arkivanov.decompose.router.stack.navigate
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.routes.WcSignTransactionRoutes

internal class WcSignTransactionContainerComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcSignTransactionModel = getOrCreateModel(params = params)
    private val contentNavigation = StackNavigation<WcSignTransactionRoutes>()
    private val contentStack = childStack(
        source = contentNavigation,
        serializer = WcSignTransactionRoutes.serializer(),
        initialConfiguration = WcSignTransactionRoutes.Transaction,
        childFactory = ::screenChild,
    )

    private fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val content by contentStack.subscribeAsState()
        TangemModalBottomSheet<WcSignTransactionRoutes>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = content.active.configuration,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = { config -> Title(route = config) },
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

    @Composable
    private fun Title(route: WcSignTransactionRoutes) {
        TangemModalBottomSheetTitle(
            title = resourceReference(R.string.wc_wallet_connect),
            startIconRes = when (route) {
                is WcSignTransactionRoutes.Transaction -> null
                else -> R.drawable.ic_back_24
            },
            onStartClick = when (route) {
                is WcSignTransactionRoutes.Transaction -> null
                else -> ::toTransaction
            },
            endIconRes = when (route) {
                is WcSignTransactionRoutes.Transaction -> R.drawable.ic_close_24
                else -> null
            },
            onEndClick = when (route) {
                is WcSignTransactionRoutes.Transaction -> ::dismiss
                else -> null
            },
        )
    }

    private fun toTransactionRequestInfo() {
        contentNavigation.navigate {
            listOf(WcSignTransactionRoutes.TransactionRequestInfo)
        }
    }

    private fun toTransaction() {
        contentNavigation.navigate {
            listOf(WcSignTransactionRoutes.Transaction)
        }
    }

    private fun screenChild(
        config: WcSignTransactionRoutes,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        is WcSignTransactionRoutes.Transaction -> WcSignTransactionComponent(
            appComponentContext = childByContext(componentContext),
            model = model,
            transactionInfoOnClick = ::toTransactionRequestInfo,
        )
        is WcSignTransactionRoutes.TransactionRequestInfo -> WcSignTransactionRequestInfoComponent(
            appComponentContext = childByContext(componentContext),
            model = model,
        )
    }

    data class Params(val rawRequest: WcSdkSessionRequest)
}