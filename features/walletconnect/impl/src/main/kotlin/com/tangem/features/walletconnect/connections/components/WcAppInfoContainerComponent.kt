package com.tangem.features.walletconnect.connections.components

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
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
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.walletconnect.connections.model.WcAppInfoModel
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.features.walletconnect.impl.R

internal class WcAppInfoContainerComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcAppInfoModel = getOrCreateModel(params = params)
    private val contentStack = childStack(
        source = model.contentNavigation,
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
            onBack = ::contentBack,
            containerColor = when (content.active.configuration) {
                is WcAppInfoRoutes.Alert,
                is WcAppInfoRoutes.SelectNetworks,
                is WcAppInfoRoutes.AppInfo,
                -> TangemTheme.colors.background.tertiary
                is WcAppInfoRoutes.SelectWallet -> TangemTheme.colors.background.primary
            },
            title = { config -> Title(route = config) },
            content = {
                Children(modifier = Modifier.animateContentSize(), stack = content) { child ->
                    child.instance.Content(modifier = Modifier)
                }
            },
        )
    }

    @Composable
    private fun Title(route: WcAppInfoRoutes) {
        TangemModalBottomSheetTitle(
            title = when (route) {
                is WcAppInfoRoutes.Alert -> null
                WcAppInfoRoutes.AppInfo -> resourceReference(R.string.wc_wallet_connect)
                WcAppInfoRoutes.SelectNetworks -> stringReference("Choose networks")
                WcAppInfoRoutes.SelectWallet -> stringReference("Choose wallet")
            },
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
        when (contentStack.value.active.configuration) {
            WcAppInfoRoutes.AppInfo -> dismiss()
            WcAppInfoRoutes.SelectNetworks -> {
                model.clearAvailableNetworks()
                model.contentNavigation.pop()
            }
            else -> model.contentNavigation.pop()
        }
    }

    private fun screenChild(config: WcAppInfoRoutes, componentContext: ComponentContext): ComposableContentComponent {
        val appComponentContext = childByContext(componentContext)
        return when (config) {
            is WcAppInfoRoutes.AppInfo -> WcAppInfoComponent(appComponentContext = appComponentContext, model = model)
            is WcAppInfoRoutes.Alert -> TODO()
            WcAppInfoRoutes.SelectNetworks -> WcSelectNetworksComponent(
                appComponentContext = appComponentContext,
                model = model,
            )
            WcAppInfoRoutes.SelectWallet -> WcSelectWalletComponent(
                appComponentContext = appComponentContext,
                model = model,
            )
        }
    }

    data class Params(val userWalletId: UserWalletId, val wcUrl: String, val source: WcPairRequest.Source)
}