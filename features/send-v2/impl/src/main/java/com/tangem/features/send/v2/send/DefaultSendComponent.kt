package com.tangem.features.send.v2.send

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.send.model.SendModel
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance

internal class DefaultSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendComponent.Params,
) : SendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<SendRoute>()

    private val innerRouter = InnerRouter<SendRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val initialRoute = SendRoute.Empty
    private val currentRoute = MutableStateFlow<SendRoute>(initialRoute)

    private val model: SendModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "sendInnerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = initialRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            createChild(
                configuration,
                childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::onChildBack)
    }

    private fun createChild(route: SendRoute, factoryContext: AppComponentContext) = when (route) {
        SendRoute.Empty -> getStubComponent()
        is SendRoute.Destination -> getDestinationComponent(factoryContext, route)
        is SendRoute.Amount -> getAmountComponent()
        is SendRoute.Fee -> getFeeComponent()
        SendRoute.Confirm -> getConfirmComponent()
    }

    private fun getDestinationComponent(factoryContext: AppComponentContext, route: SendRoute) =
        SendDestinationComponent(
            appComponentContext = factoryContext,
            params = SendDestinationComponent.Params(
                state = model.uiState.value.destinationUM,
                currentRoute = currentRoute.filterIsInstance<SendRoute.Destination>(),
                analyticsCategoryName = SendAnalyticEvents.SEND_CATEGORY,
                userWallet = model.userWallet,
                cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                callback = model,
                isEditMode = route.isEditMode,
            ),
        )

    private fun getAmountComponent() = getStubComponent() // todo

    private fun getFeeComponent() = getStubComponent() // todo

    private fun getConfirmComponent() = getStubComponent() // todo

    private fun getStubComponent() = ComposableContentComponent { }

    private fun onChildBack() {
        if (childStack.value.active.configuration == SendRoute.Empty || childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : SendComponent.Factory {
        override fun create(context: AppComponentContext, params: SendComponent.Params): DefaultSendComponent
    }
}