package com.tangem.features.tokenreceive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.tokenreceive.model.TokenReceiveModel
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes
import com.tangem.features.tokenreceive.ui.TokenReceiveContentSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTokenReceiveComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TokenReceiveComponent.Params,
) : TokenReceiveComponent, AppComponentContext by appComponentContext {

    private val model: TokenReceiveModel = getOrCreateModel(params)
    private val innerRouter = InnerRouter<TokenReceiveRoutes>(
        stackNavigation = model.stackNavigation,
        popCallback = { onChildBack() },
    )

    private val contentStack = childStack(
        key = "TokenReceiveFlowStack",
        source = model.stackNavigation,
        serializer = TokenReceiveRoutes.serializer(),
        initialConfiguration = getInitialConfig(model.params.config.shouldShowWarning),
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    @Composable
    override fun BottomSheet() {
        val content by contentStack.subscribeAsState()
        val currentRoute = content.active.configuration

        TokenReceiveContentSheet(
            route = currentRoute,
            onCloseClick = ::dismiss,
            onBackClick = ::onChildBack,
            contentStack = content,
        )
    }

    override fun dismiss() {
        model.params.onDismiss()
    }

    private fun onChildBack() {
        when (contentStack.value.active.configuration) {
            is TokenReceiveRoutes.QrCode -> model.stackNavigation.pop()
            TokenReceiveRoutes.ReceiveAssets,
            TokenReceiveRoutes.Warning,
            -> dismiss()
        }
    }

    private fun screenChild(
        config: TokenReceiveRoutes,
        componentContext: ComponentContext,
    ): ComposableContentComponent {
        val appComponentContext = childByContext(
            componentContext = componentContext,
            router = innerRouter,
        )
        return when (config) {
            is TokenReceiveRoutes.QrCode -> TokenReceiveQrCodeComponent(
                appComponentContext = appComponentContext,
                params = TokenReceiveQrCodeComponent.TokenReceiveQrCodeParams(
                    network = model.params.config.cryptoCurrency.network.name,
                    address = model.state.value.addresses[config.addressId] ?: error("Address has to be there"),
                    callback = model,
                    onDismiss = ::dismiss,
                    id = config.addressId,
                ),
            )
            TokenReceiveRoutes.ReceiveAssets -> TokenReceiveAssetsComponent(
                appComponentContext = appComponentContext,
                params = TokenReceiveAssetsComponent.TokenReceiveAssetsParams(
                    addresses = model.state.value.addresses,
                    callback = model,
                    onDismiss = ::dismiss,
                    notificationConfigs = model.state.value.notificationConfigs,
                    showMemoDisclaimer = model.params.config.showMemoDisclaimer,
                    fullName = model.params.config.cryptoCurrency.network.name,
                    tokenName = model.getTokenName(),
                ),
            )
            TokenReceiveRoutes.Warning -> TokenReceiveWarningComponent(
                appComponentContext = appComponentContext,
                params = TokenReceiveWarningComponent.TokenReceiveWarningParams(
                    iconState = model.state.value.iconState,
                    callback = model,
                    onDismiss = ::dismiss,
                    network = model.params.config.cryptoCurrency.network,
                ),
            )
        }
    }

    private fun getInitialConfig(shouldShowWarning: Boolean): TokenReceiveRoutes {
        return if (shouldShowWarning) {
            TokenReceiveRoutes.Warning
        } else {
            TokenReceiveRoutes.ReceiveAssets
        }
    }

    @AssistedFactory
    interface Factory : TokenReceiveComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TokenReceiveComponent.Params,
        ): DefaultTokenReceiveComponent
    }
}

internal interface TokenReceiveModelCallback :
    TokenReceiveAssetsComponent.TokenReceiveAssetsModelCallback,
    TokenReceiveQrCodeComponent.TokenReceiveQrCodeModelCallback,
    TokenReceiveWarningComponent.TokenReceiveWarningModelCallback