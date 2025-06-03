package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.features.walletconnect.connections.model.WcAppInfoModel
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.features.walletconnect.impl.R

internal class WcAppInfoContainerComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcAppInfoModel = getOrCreateModel(params = params)
    private val contentNavigation = StackNavigation<WcAppInfoRoutes>()
    private val contentStack = childStack(
        source = contentNavigation,
        serializer = WcAppInfoRoutes.serializer(),
        initialConfiguration = WcAppInfoRoutes.AppInfo,
        childFactory = ::screenChild,
    )

    private fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val content by contentStack.subscribeAsState()
        TangemModalBottomSheet<WcAppInfoRoutes>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = content.active.configuration,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = { config -> Title(route = config) },
            content = {
                Children(stack = content, animation = stackAnimation()) { child ->
                    child.instance.Content(modifier = Modifier)
                }
            },
        )
    }

    @Composable
    private fun Title(route: WcAppInfoRoutes) {
        TangemModalBottomSheetTitle(
            title = resourceReference(R.string.wc_wallet_connect),
            startIconRes = when (route) {
                is WcAppInfoRoutes.AppInfo, is WcAppInfoRoutes.Alert -> null
                is WcAppInfoRoutes.SelectNetworks, is WcAppInfoRoutes.SelectWallet -> R.drawable.ic_back_24
            },
            onStartClick = when (route) {
                is WcAppInfoRoutes.AppInfo, is WcAppInfoRoutes.Alert -> null
                is WcAppInfoRoutes.SelectNetworks, is WcAppInfoRoutes.SelectWallet -> ::contentBack
            },
            endIconRes = when (route) {
                is WcAppInfoRoutes.AppInfo, is WcAppInfoRoutes.Alert -> R.drawable.ic_close_24
                is WcAppInfoRoutes.SelectNetworks, is WcAppInfoRoutes.SelectWallet -> null
            },
            onEndClick = when (route) {
                is WcAppInfoRoutes.AppInfo -> ::dismiss
                is WcAppInfoRoutes.Alert -> ::contentBack
                is WcAppInfoRoutes.SelectNetworks,
                is WcAppInfoRoutes.SelectWallet,
                -> null
            },
        )
    }

    private fun contentBack() {
        contentNavigation.pop()
    }

    private fun screenChild(config: WcAppInfoRoutes, componentContext: ComponentContext): ComposableContentComponent =
        when (config) {
            is WcAppInfoRoutes.AppInfo -> WcAppInfoComponent(
                appComponentContext = childByContext(componentContext),
                model = model,
            )
            is WcAppInfoRoutes.Alert -> TODO()
            WcAppInfoRoutes.SelectNetworks -> TODO()
            WcAppInfoRoutes.SelectWallet -> TODO()
        }

    data class Params(val wcUrl: String, val source: WcPairRequest.Source)
}