package com.tangem.features.send.v2.entrypoint

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.SendEntryPointComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.entrypoint.model.SendEntryPointModel
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendEntryPointComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendEntryPointComponent.Params,
    sendWithSwapComponentFactory: SendWithSwapComponent.Factory,
    sendComponentFactory: SendComponent.Factory,
    private val chooseManagedTokensComponentFactory: ChooseManagedTokensComponent.Factory,
) : SendEntryPointComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<SendEntryRoute>()

    private val innerRouter = InnerRouter<SendEntryRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: SendEntryPointModel = getOrCreateModel(params = params, router = innerRouter)

    private val sendWithSwapComponent = sendWithSwapComponentFactory.create(
        context = child("sendEntrySendWithSwap"),
        params = SendWithSwapComponent.Params(
            userWalletId = params.userWalletId,
            currency = params.cryptoCurrency,
            callback = model,
        ),
    )

    private val sendComponent = sendComponentFactory.create(
        context = child("sendEntryVanillaSend"),
        params = SendComponent.Params(
            userWalletId = params.userWalletId,
            currency = params.cryptoCurrency,
            callback = model,
        ),
    )

    private val childStack = childStack(
        key = "sendEntryStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = SendEntryRoute.Send,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            getChildComponent(
                configuration = configuration,
                factoryContext = childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStackValue by childStack.subscribeAsState()

        Children(
            stack = childStackValue,
            animation = stackAnimation { child ->
                when (child.configuration) {
                    SendEntryRoute.Send,
                    SendEntryRoute.SendWithSwap,
                    -> fade()
                    is SendEntryRoute.ChooseToken -> slide(orientation = Orientation.Vertical) + fade()
                }
            },
        ) { child ->
            child.instance.Content(modifier.fillMaxSize())
        }
    }

    private fun getChildComponent(
        configuration: SendEntryRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent = when (configuration) {
        is SendEntryRoute.ChooseToken -> getManagedTokensComponent(
            componentContext = factoryContext,
            showSendViaSwapNotification = configuration.showSendViaSwapNotification,
        )
        SendEntryRoute.Send -> sendComponent
        SendEntryRoute.SendWithSwap -> sendWithSwapComponent
    }

    private fun getManagedTokensComponent(
        componentContext: ComponentContext,
        showSendViaSwapNotification: Boolean,
    ): ChooseManagedTokensComponent {
        return chooseManagedTokensComponentFactory.create(
            context = childByContext(componentContext),
            params = ChooseManagedTokensComponent.Params(
                userWalletId = params.userWalletId,
                initialCurrency = params.cryptoCurrency,
                source = ChooseManagedTokensComponent.Source.SendViaSwap,
                selectedCurrency = null,
                showSendViaSwapNotification = showSendViaSwapNotification,
                callback = model,
                analyticsCategoryName = CommonSendAnalyticEvents.SEND_CATEGORY,
            ),
        )
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : SendEntryPointComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendEntryPointComponent.Params,
        ): DefaultSendEntryPointComponent
    }
}