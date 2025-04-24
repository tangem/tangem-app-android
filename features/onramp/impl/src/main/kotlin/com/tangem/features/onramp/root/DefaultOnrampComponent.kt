package com.tangem.features.onramp.root

import androidx.activity.compose.BackHandler
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
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.redirect.OnrampRedirectComponent
import com.tangem.features.onramp.root.entity.OnrampChild
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultOnrampComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: OnrampComponent.Params,
    private val settingsComponentFactory: OnrampSettingsComponent.Factory,
    private val onrampMainComponentFactory: OnrampMainComponent.Factory,
    private val onrampRedirectComponentFactory: OnrampRedirectComponent.Factory,
) : OnrampComponent, AppComponentContext by context {

    private val navigation = StackNavigation<OnrampChild>()
    private val contentStack = childStack(
        key = "onramp_content_stack",
        source = navigation,
        serializer = OnrampChild.serializer(),
        initialConfiguration = OnrampChild.Main,
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by contentStack.subscribeAsState()

        BackHandler(onBack = router::pop)
        Children(
            stack = childStack,
            animation = stackAnimation(),
        ) { child ->
            child.instance.Content(modifier = modifier)
        }
    }

    private fun screenChild(config: OnrampChild, componentContext: ComponentContext): ComposableContentComponent =
        when (config) {
            OnrampChild.Settings -> settingsComponentFactory.create(
                context = childByContext(componentContext),
                params = OnrampSettingsComponent.Params(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = params.cryptoCurrency,
                    onBack = navigation::pop,
                ),
            )
            OnrampChild.Main -> onrampMainComponentFactory.create(
                context = childByContext(componentContext),
                params = OnrampMainComponent.Params(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = params.cryptoCurrency,
                    openSettings = { navigation.push(OnrampChild.Settings) },
                    source = params.source,
                    openRedirectPage = {
                        navigation.push(
                            OnrampChild.RedirectPage(
                                quote = it,
                                cryptoCurrency = params.cryptoCurrency,
                            ),
                        )
                    },
                ),
            )
            is OnrampChild.RedirectPage -> onrampRedirectComponentFactory.create(
                context = childByContext(componentContext),
                params = OnrampRedirectComponent.Params(
                    userWalletId = params.userWalletId,
                    onBack = navigation::pop,
                    cryptoCurrency = config.cryptoCurrency,
                    onrampProviderWithQuote = config.quote,
                ),
            )
        }

    @AssistedFactory
    interface Factory : OnrampComponent.Factory {
        override fun create(context: AppComponentContext, params: OnrampComponent.Params): DefaultOnrampComponent
    }
}