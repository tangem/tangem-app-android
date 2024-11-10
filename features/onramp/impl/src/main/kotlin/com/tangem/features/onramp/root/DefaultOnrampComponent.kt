package com.tangem.features.onramp.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.root.entity.OnrampChild
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultOnrampComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: OnrampComponent.Params,
    private val settingsComponentFactory: OnrampSettingsComponent.Factory,
) : OnrampComponent, AppComponentContext by context {

    private val navigation = StackNavigation<OnrampChild>()
    private val contentStack = childStack(
        key = "onramp_content_stack",
        source = navigation,
        serializer = OnrampChild.serializer(),
        initialConfiguration = OnrampChild.Settings,
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by contentStack.subscribeAsState()

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
                // params = OnrampSettingsComponent.Params(navigation::pop) // todo uncomment after https://tangem.atlassian.net/browse/AND-8407
                params = OnrampSettingsComponent.Params(router::pop),
            )
            OnrampChild.Main -> TODO("https://tangem.atlassian.net/browse/AND-8407")
        }

    @AssistedFactory
    interface Factory : OnrampComponent.Factory {
        override fun create(context: AppComponentContext, params: OnrampComponent.Params): DefaultOnrampComponent
    }
}
