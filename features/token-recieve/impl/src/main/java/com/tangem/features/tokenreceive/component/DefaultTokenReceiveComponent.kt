package com.tangem.features.tokenreceive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.tokenreceive.model.TokenReceiveModel
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes
import com.tangem.features.tokenreceive.ui.TokenReceiveContent
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

@Composable
internal fun TokenReceiveContentSheet(
    route: TokenReceiveRoutes,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    contentStack: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onCloseClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBackClick,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            Title(
                route = route,
                onBackClick = onBackClick,
                onCloseClick = onCloseClick,
            )
        },
        content = {
            TokenReceiveContent(
                stackState = contentStack,
                modifier = Modifier,
            )
        },
    )
}

@Composable
private fun Title(route: TokenReceiveRoutes, onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    when (route) {
        is TokenReceiveRoutes.QrCode -> {
            TangemModalBottomSheetTitle(
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onBackClick,
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onCloseClick,
            )
        }
        TokenReceiveRoutes.ReceiveAssets -> {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.domain_receive_assets_navigation_title),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onCloseClick,
            )
        }
        TokenReceiveRoutes.Warning -> {
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onCloseClick,
            )
        }
    }
}

internal interface TokenReceiveModelCallback :
    TokenReceiveAssetsComponent.TokenReceiveAssetsModelCallback,
    TokenReceiveQrCodeComponent.TokenReceiveQrCodeModelCallback,
    TokenReceiveWarningComponent.TokenReceiveWarningModelCallback